<script setup lang="ts">
import LoginPanel from './components/LoginPanel.vue'
</script>

<template>
  <main class="grid min-h-screen place-items-center overflow-hidden bg-slate-100 px-6 py-10">
    <div class="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,#bae6fd_0,transparent_30%),radial-gradient(circle_at_80%_10%,#ddd6fe_0,transparent_28%),linear-gradient(135deg,#f8fafc,#e2e8f0)]" />
    <LoginPanel class="relative" />
  </main>
</template>
