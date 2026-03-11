[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://127.0.0.1:8090',
    [ValidateSet('chat', 'follow', 'goods', 'errand')]
    [string]$Scenario = 'chat',
    [int]$Concurrency = 20,
    [int]$RequestsPerWorker = 50,
    [int]$TimeoutSeconds = 15,
    [string]$AccessToken,
    [string]$RefreshToken,
    [string]$CsrfToken,
    [string]$Origin = 'http://localhost:5173',
    [string]$Referer = 'http://localhost:5173/',
    [long]$ReceiverId = 2,
    [long]$TargetUserId = 2,
    [int]$CategoryId = 1,
    [decimal]$Price = 9.90,
    [string]$SchoolCode = 'SC001',
    [string]$CampusCode = 'CP001',
    [int]$ThinkTimeMs = 0,
    [string]$OutputCsvPath,
    [switch]$Preview
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function New-ScenarioRequest {
    param(
        [string]$ScenarioName,
        [int]$WorkerId,
        [int]$Iteration,
        [long]$ReceiverIdValue,
        [long]$TargetUserIdValue,
        [int]$CategoryIdValue,
        [decimal]$PriceValue,
        [string]$SchoolCodeValue,
        [string]$CampusCodeValue
    )

    switch ($ScenarioName) {
        'chat' {
            return [pscustomobject]@{
                Method = 'POST'
                Path = '/chat/send'
                Body = @{
                    receiverId = $ReceiverIdValue
                    content = "perf-chat-$WorkerId-$Iteration"
                    type = 0
                }
            }
        }
        'follow' {
            return [pscustomobject]@{
                Method = 'POST'
                Path = "/user/follow/$TargetUserIdValue"
                Body = $null
            }
        }
        'goods' {
            return [pscustomobject]@{
                Method = 'POST'
                Path = '/goods'
                Body = @{
                    title = "压测商品-$WorkerId-$Iteration"
                    categoryId = $CategoryIdValue
                    price = [double]$PriceValue
                    description = 'pressure test goods publish'
                    image = 'https://example.com/perf.png'
                    imageList = '[]'
                    itemCondition = 9
                    tradeType = 0
                }
            }
        }
        'errand' {
            return [pscustomobject]@{
                Method = 'POST'
                Path = '/errand'
                Body = @{
                    title = "压测跑腿-$WorkerId-$Iteration"
                    description = 'pressure test errand publish'
                    taskContent = '请帮忙取件并送达'
                    pickupAddress = '压测取件点'
                    deliveryAddress = '压测送达点'
                    reward = 5.5
                    deadline = ([DateTime]::Now.AddHours(2).ToString('yyyy-MM-dd HH:mm:ss'))
                    schoolCode = $SchoolCodeValue
                    campusCode = $CampusCodeValue
                }
            }
        }
    }
}

function Get-Percentile {
    param(
        [double[]]$Values,
        [double]$Percent
    )

    $items = @($Values)
    if (-not $items -or $items.Count -eq 0) {
        return 0
    }
    $sorted = @($items | Sort-Object)
    $rank = [math]::Ceiling(($Percent / 100.0) * $sorted.Count) - 1
    $rank = [math]::Max(0, [math]::Min($rank, $sorted.Count - 1))
    return [math]::Round([double]$sorted[$rank], 2)

}
$scenarioPreview = New-ScenarioRequest -ScenarioName $Scenario -WorkerId 1 -Iteration 1 -ReceiverIdValue $ReceiverId -TargetUserIdValue $TargetUserId -CategoryIdValue $CategoryId -PriceValue $Price -SchoolCodeValue $SchoolCode -CampusCodeValue $CampusCode
$normalizedBaseUrl = $BaseUrl.TrimEnd('/' )

if ($Preview) {
    [pscustomobject]@{
        BaseUrl = $BaseUrl
        Scenario = $Scenario
        Concurrency = $Concurrency
        RequestsPerWorker = $RequestsPerWorker
        SampleMethod = $scenarioPreview.Method
        SamplePath = $scenarioPreview.Path
        SampleBody = if ($scenarioPreview.Body) { ($scenarioPreview.Body | ConvertTo-Json -Depth 6) } else { $null }
    } | Format-List
    exit 0
}

$startedAt = Get-Date
$results = 1..$Concurrency | ForEach-Object -Parallel {
    param($workerId)

    function Local-NewHttpClientContext {
        param($BaseUri, $AccessTokenValue, $RefreshTokenValue, $ProvidedCsrfToken, $OriginValue, $RefererValue, $TimeoutSecondsValue)
        $handler = [System.Net.Http.HttpClientHandler]::new()
        $handler.UseCookies = $true
        $handler.CookieContainer = [System.Net.CookieContainer]::new()
        if ($AccessTokenValue) {
            $handler.CookieContainer.Add($BaseUri, [System.Net.Cookie]::new('access_token', $AccessTokenValue, '/'))
        }
        if ($RefreshTokenValue) {
            $handler.CookieContainer.Add($BaseUri, [System.Net.Cookie]::new('refresh_token', $RefreshTokenValue, '/'))
        }
        if ($ProvidedCsrfToken) {
            $handler.CookieContainer.Add($BaseUri, [System.Net.Cookie]::new('csrf_token', $ProvidedCsrfToken, '/'))
        }
        $client = [System.Net.Http.HttpClient]::new($handler)
        $client.Timeout = [TimeSpan]::FromSeconds($TimeoutSecondsValue)
        $client.DefaultRequestHeaders.Add('X-Requested-With', 'XMLHttpRequest')
        if ($OriginValue) { $client.DefaultRequestHeaders.Add('Origin', $OriginValue) }
        if ($RefererValue) { $client.DefaultRequestHeaders.Add('Referer', $RefererValue) }
        return [pscustomobject]@{ Client = $client; Handler = $handler }
    }

    function Local-GetCsrfCookie {
        param($CookieContainer, $BaseUri)
        foreach ($cookie in $CookieContainer.GetCookies($BaseUri)) {
            if ($cookie.Name -eq 'csrf_token') { return $cookie.Value }
        }
        return $null
    }

    function Local-InitCsrf {
        param($Client, $CookieContainer, $BaseUri, $ExistingToken)
        if ($ExistingToken) { return $ExistingToken }
        $bootstrapUri = [Uri]::new($BaseUri, '/goods?pageNum=1&pageSize=1')
        $response = $Client.GetAsync($bootstrapUri).GetAwaiter().GetResult()
        $null = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $response.Dispose()
        $token = Local-GetCsrfCookie -CookieContainer $CookieContainer -BaseUri $BaseUri
        if (-not $token) { throw '未获取到 csrf_token Cookie。' }
        return $token
    }

    function Local-NewScenarioRequest {
        param($ScenarioName, $WorkerIdValue, $IterationValue, $ReceiverIdValue, $TargetUserIdValue, $CategoryIdValue, $PriceValue, $SchoolCodeValue, $CampusCodeValue)
        switch ($ScenarioName) {
            'chat' {
                return [pscustomobject]@{ Method = 'POST'; Path = '/chat/send'; Body = @{ receiverId = $ReceiverIdValue; content = "perf-chat-$WorkerIdValue-$IterationValue"; type = 0 } }
            }
            'follow' {
                return [pscustomobject]@{ Method = 'POST'; Path = "/user/follow/$TargetUserIdValue"; Body = $null }
            }
            'goods' {
                return [pscustomobject]@{ Method = 'POST'; Path = '/goods'; Body = @{ title = "压测商品-$WorkerIdValue-$IterationValue"; categoryId = $CategoryIdValue; price = [double]$PriceValue; description = 'pressure test goods publish'; image = 'https://example.com/perf.png'; imageList = '[]'; itemCondition = 9; tradeType = 0 } }
            }
            'errand' {
                return [pscustomobject]@{ Method = 'POST'; Path = '/errand'; Body = @{ title = "压测跑腿-$WorkerIdValue-$IterationValue"; description = 'pressure test errand publish'; taskContent = '请帮忙取件并送达'; pickupAddress = '压测取件点'; deliveryAddress = '压测送达点'; reward = 5.5; deadline = ([DateTime]::Now.AddHours(2).ToString('yyyy-MM-dd HH:mm:ss')); schoolCode = $SchoolCodeValue; campusCode = $CampusCodeValue } }
            }
        }
    }

    function Local-InvokeOnce {
        param($Client, $BaseUri, $CsrfTokenValue, $ScenarioRequest)
        $uri = [Uri]::new($BaseUri, $ScenarioRequest.Path)
        $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::$($ScenarioRequest.Method), $uri)
        if ($CsrfTokenValue) { $request.Headers.Add('X-CSRF-TOKEN', $CsrfTokenValue) }
        if ($ScenarioRequest.Body -ne $null) {
            $json = $ScenarioRequest.Body | ConvertTo-Json -Depth 6 -Compress
            $request.Content = [System.Net.Http.StringContent]::new($json, [System.Text.Encoding]::UTF8, 'application/json')
        }
        $watch = [System.Diagnostics.Stopwatch]::StartNew()
        try {
            $response = $Client.SendAsync($request).GetAwaiter().GetResult()
            $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            $watch.Stop()
            $ok = $response.IsSuccessStatusCode
            $appCode = $null
            $appMessage = $null
            if ($body) {
                try {
                    $payload = $body | ConvertFrom-Json -Depth 8
                    $appCode = $payload.code
                    $appMessage = $payload.message
                    if ($appCode -ne $null -and [int]$appCode -ne 200) {
                        $ok = $false
                    }
                } catch {}
            }
            $statusCode = [int]$response.StatusCode
            $response.Dispose()
            return [pscustomobject]@{ Success = $ok; HttpStatus = $statusCode; AppCode = $appCode; Message = $appMessage; LatencyMs = [math]::Round($watch.Elapsed.TotalMilliseconds, 2) }
        } catch {
            $watch.Stop()
            return [pscustomobject]@{ Success = $false; HttpStatus = 0; AppCode = $null; Message = $_.Exception.Message; LatencyMs = [math]::Round($watch.Elapsed.TotalMilliseconds, 2) }
        } finally {
            $request.Dispose()
        }
    }

$baseUri = [Uri]$using:normalizedBaseUrl
    $ctx = Local-NewHttpClientContext -BaseUri $baseUri -AccessTokenValue $using:AccessToken -RefreshTokenValue $using:RefreshToken -ProvidedCsrfToken $using:CsrfToken -OriginValue $using:Origin -RefererValue $using:Referer -TimeoutSecondsValue $using:TimeoutSeconds
    $client = $ctx.Client
    $csrfTokenValue = Local-InitCsrf -Client $client -CookieContainer $ctx.Handler.CookieContainer -BaseUri $baseUri -ExistingToken $using:CsrfToken

    $workerResults = New-Object System.Collections.Generic.List[object]
    for ($iteration = 1; $iteration -le $using:RequestsPerWorker; $iteration++) {
        $requestSpec = Local-NewScenarioRequest -ScenarioName $using:Scenario -WorkerIdValue $workerId -IterationValue $iteration -ReceiverIdValue $using:ReceiverId -TargetUserIdValue $using:TargetUserId -CategoryIdValue $using:CategoryId -PriceValue $using:Price -SchoolCodeValue $using:SchoolCode -CampusCodeValue $using:CampusCode
        $result = Local-InvokeOnce -Client $client -BaseUri $baseUri -CsrfTokenValue $csrfTokenValue -ScenarioRequest $requestSpec
        $workerResults.Add([pscustomobject]@{
            WorkerId = $workerId
            Iteration = $iteration
            Success = $result.Success
            HttpStatus = $result.HttpStatus
            AppCode = $result.AppCode
            Message = $result.Message
            LatencyMs = $result.LatencyMs
            Path = $requestSpec.Path
            Method = $requestSpec.Method
        })
        if ($using:ThinkTimeMs -gt 0) {
            Start-Sleep -Milliseconds $using:ThinkTimeMs
        }
    }
    $client.Dispose()
    return $workerResults
} -ThrottleLimit $Concurrency

$finishedAt = Get-Date
$flatResults = @($results)

if ($OutputCsvPath) {
    $flatResults | Export-Csv -Path $OutputCsvPath -NoTypeInformation -Encoding UTF8
}

$total = $flatResults.Count
$success = @($flatResults | Where-Object { $_.Success }).Count
$failed = $total - $success
$latencies = @($flatResults | ForEach-Object { [double]$_.LatencyMs })
$durationSeconds = [math]::Max(($finishedAt - $startedAt).TotalSeconds, 0.001)
$rps = [math]::Round($total / $durationSeconds, 2)
$statusSummary = $flatResults | Group-Object HttpStatus | Sort-Object Name | ForEach-Object { "{0}={1}" -f $_.Name, $_.Count }
$appSummary = $flatResults | Group-Object AppCode | Sort-Object Name | ForEach-Object { "{0}={1}" -f $_.Name, $_.Count }

"`n=== 风控压测结果 ==="
"场景: $Scenario"
"开始时间: $($startedAt.ToString('yyyy-MM-dd HH:mm:ss'))"
"结束时间: $($finishedAt.ToString('yyyy-MM-dd HH:mm:ss'))"
"总请求数: $total"
"成功数: $success"
"失败数: $failed"
"平均耗时(ms): $([math]::Round((($latencies | Measure-Object -Average).Average), 2))"
"P50(ms): $(Get-Percentile -Values $latencies -Percent 50)"
"P95(ms): $(Get-Percentile -Values $latencies -Percent 95)"
"P99(ms): $(Get-Percentile -Values $latencies -Percent 99)"
"吞吐量(req/s): $rps"
"HTTP状态分布: $($statusSummary -join ', ')"
"业务码分布: $($appSummary -join ', ')"
if ($OutputCsvPath) {
    "明细输出: $OutputCsvPath"
}



