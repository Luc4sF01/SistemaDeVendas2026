/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#7C3AED',
          50: '#F5F3FF',
          100: '#EDE9FE',
          200: '#DDD6FE',
          300: '#C4B5FD',
          400: '#A78BFA',
          500: '#8B5CF6',
          600: '#7C3AED',
          700: '#6D28D9',
          800: '#5B21B6',
          900: '#4C1D95',
        },
        success: {
          DEFAULT: '#16A34A',
          light: '#DCFCE7',
        },
        danger: {
          DEFAULT: '#C82828',
          light: '#fdeaea',
        },
        warning: {
          DEFAULT: '#C88200',
          light: '#fdf3e0',
        },
        background: '#F8F7FF',
      },
    },
  },
  plugins: [],
}
