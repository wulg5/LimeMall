<template>
  <main class="not-found">
    <section>
      <div class="code" aria-hidden="true">404</div>
      <h1>找不到这个页面</h1>
      <p>链接可能已更新，也可以返回商城继续选购。</p>
      <router-link class="ui-link-button" to="/shop">返回商城首页</router-link>
    </section>
  </main>
</template>

<style scoped>
.not-found { min-height: 100dvh; display: grid; place-items: center; padding: 16px; }
.not-found section { width: min(100%, 620px); padding: clamp(20px, 6vw, 56px); border: 1px solid var(--line); border-radius: var(--radius-panel); background: var(--paper); }
.code { font-size: clamp(55px, 12vw, 115px); font-weight: 750; line-height: 1; color: var(--action); }
h1 { margin: 12px 0 7px; font-size: clamp(24px, 4vw, 40px); color: var(--pine); }
p { margin: 0 0 19px; color: var(--muted); }
</style>
