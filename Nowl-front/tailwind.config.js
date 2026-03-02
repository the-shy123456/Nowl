/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        warm: {
          50: '#FBF8F6',
          100: '#EFEBE9',
          200: '#D7CCC8',
          300: '#BCAAA4',
          400: '#A1887F',
          500: '#8D6E63',
          600: '#6D4C41',
          700: '#5D4037',
          800: '#4E342E',
          900: '#3E2723'
        },
        um: {
          primary: '#8D6E63',
          primary600: '#6D4C41',
          primary100: '#EFEBE9',
          accent: '#FF7043',
          cta: '#22c55e',
          bg: '#FFFBF8',
          text: '#3E2723',
          muted: '#8D7B75',
        }
      },
      fontFamily: {
        display: ['"PingFang SC"', '"Microsoft YaHei"', '"Noto Sans SC"', 'sans-serif'],
        body: ['"PingFang SC"', '"Microsoft YaHei"', '"Noto Sans SC"', 'sans-serif'],
      },
      boxShadow: {
        um: '0 4px 12px rgba(62, 39, 35, 0.10)',
        umSoft: '0 2px 8px rgba(62, 39, 35, 0.08)',
      },
      borderRadius: {
        um: '16px',
        ummd: '14px',
        umsm: '10px',
      },
    },
  },
  plugins: [],
}
