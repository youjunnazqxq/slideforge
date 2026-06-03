<template>
  <div class="min-h-screen bg-slate-50 text-slate-950">
    <aside class="fixed inset-y-0 left-0 w-56 border-r border-slate-200 bg-white p-4">
      <div class="text-lg font-semibold">SlideForge</div>
      <nav class="mt-6 grid gap-2 text-sm">
        <RouterLink to="/app/workspace">工作台</RouterLink>
        <RouterLink to="/app/ppt">PPT 生成</RouterLink>
        <RouterLink to="/app/profile">用户信息</RouterLink>
        <RouterLink to="/app/settings">设置</RouterLink>
      </nav>
    </aside>
    <main class="ml-56 min-h-screen p-6">
      <RouterView />
    </main>
  </div>
</template>
