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
          DEFAULT: '#3258A0',
          50: '#EBF0F9',
          100: '#C2D0EE',
          200: '#99B0E3',
          300: '#7090D8',
          400: '#4770CD',
          500: '#3258A0',
          600: '#274680',
          700: '#1C3460',
          800: '#112240',
          900: '#061020',
        },
        success: {
          DEFAULT: '#007830',
          light: '#e6f4ec',
        },
        danger: {
          DEFAULT: '#C82828',
          light: '#fdeaea',
        },
        warning: {
          DEFAULT: '#C88200',
          light: '#fdf3e0',
        },
        background: '#F5F7FA',
      },
    },
  },
  plugins: [],
}
