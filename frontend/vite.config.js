import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  optimizeDeps: {
    include: [
      'react-simple-code-editor',
      'prismjs',
      'prismjs/components/prism-javascript',
      'prismjs/components/prism-clike',
      'prismjs/components/prism-markup',
      'prismjs/components/prism-markup-templating',
      'prismjs/components/prism-java',
      'prismjs/components/prism-python',
      'prismjs/components/prism-go',
      'prismjs/components/prism-sql',
      'prismjs/components/prism-php',
    ],
  },
  server: {
    port: 5173,
    headers: {
      'Cross-Origin-Opener-Policy': 'unsafe-none'
    },
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
      },
    },
  },
})
