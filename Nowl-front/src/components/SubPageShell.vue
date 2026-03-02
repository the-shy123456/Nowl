<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import { navigateBack } from '@/utils/navigation'

const props = withDefaults(defineProps<{
  title: string
  subtitle?: string
  backTo?: string
  showBack?: boolean
  maxWidth?: 'sm' | 'md' | 'lg'
  useCard?: boolean
  cardClass?: string
}>(), {
  showBack: true,
  maxWidth: 'md',
  useCard: true,
  cardClass: '',
})

const router = useRouter()
const route = useRoute()

const maxWidthClass = computed(() => {
  if (props.maxWidth === 'sm') return 'max-w-xl'
  if (props.maxWidth === 'lg') return 'max-w-4xl'
  return 'max-w-2xl'
})

const handleBack = () => {
  navigateBack(router, route, props.backTo || '/')
}
</script>

<template>
  <div class="sub-shell">
    <div class="sub-container" :class="maxWidthClass">
      <button
        v-if="showBack"
        type="button"
        @click="handleBack"
        class="sub-back"
        aria-label="返回"
      >
        <ArrowLeft class="w-4 h-4" />
      </button>

      <div class="sub-header" :class="{ 'with-back': showBack }">
        <div v-if="$slots.icon" class="sub-icon">
          <slot name="icon" />
        </div>
        <div v-if="$slots.badge" class="sub-badge">
          <slot name="badge" />
        </div>
        <h1 class="sub-title">{{ title }}</h1>
        <p v-if="subtitle" class="sub-subtitle">{{ subtitle }}</p>
      </div>

      <div v-if="useCard" class="um-card sub-card" :class="cardClass">
        <slot />
      </div>
      <slot v-else />
    </div>
  </div>
</template>

<style scoped>
.sub-shell {
  min-height: calc(100vh - 70px);
  background: var(--um-bg);
  padding: 18px 0 56px;
  position: relative;
  overflow: hidden;
}

.sub-shell::before,
.sub-shell::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  opacity: 0.45;
}

.sub-shell::before {
  width: 320px;
  height: 320px;
  top: -160px;
  left: -120px;
  background: radial-gradient(circle at center, rgba(255, 143, 0, 0.22), rgba(255, 143, 0, 0));
}

.sub-shell::after {
  width: 360px;
  height: 360px;
  right: -140px;
  top: -180px;
  background: radial-gradient(circle at center, rgba(141, 110, 99, 0.2), rgba(141, 110, 99, 0));
}

.sub-container {
  position: relative;
  z-index: 1;
  margin: 0 auto;
  padding: 0 16px;
}

.sub-back {
  position: absolute;
  left: 16px;
  top: 6px;
  z-index: 10;
  width: 40px;
  height: 40px;
  border-radius: 999px;
  border: 1px solid var(--um-border);
  background: #fff;
  color: var(--um-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease, border-color 0.2s ease, background-color 0.2s ease;
}

.sub-back:hover {
  color: var(--um-accent);
  border-color: rgba(255, 112, 67, 0.35);
  background: #fff7f3;
}

.sub-header {
  text-align: center;
  margin-bottom: 24px;
}

.sub-header.with-back {
  padding-top: 52px;
}

.sub-icon {
  width: 60px;
  height: 60px;
  border-radius: 14px;
  background: var(--um-accent);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  box-shadow: 0 4px 10px rgba(255, 112, 67, 0.3);
  animation: float-in 0.45s ease;
}

.sub-badge {
  display: flex;
  justify-content: center;
  margin-bottom: 8px;
}

.sub-title {
  font-size: clamp(1.75rem, 3vw, 2.1rem);
  line-height: 1.2;
  color: var(--um-text);
  font-weight: 700;
  margin: 0;
}

.sub-subtitle {
  margin-top: 6px;
  font-size: 0.92rem;
  color: var(--um-muted);
  max-width: 620px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.6;
}

.sub-card {
  padding: 26px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), #fff);
}

@keyframes float-in {
  from {
    transform: translateY(8px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .sub-shell {
    padding-top: 12px;
  }

  .sub-card {
    padding: 18px;
  }
}
</style>
