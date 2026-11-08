<script setup lang="ts">
import { useRoute, RouterView } from 'vue-router'
import AppSidebar from './components/AppSidebar.vue'
import AppHeader from './components/AppHeader.vue'

const route = useRoute()
</script>

<template>
  <div class="flex h-screen w-full overflow-hidden bg-[#f6f8fb]">
    <AppSidebar />

    <div class="flex min-w-0 flex-1 flex-col">
      <AppHeader />

      <main class="flex-1 overflow-y-auto px-4 py-4 sm:px-5 sm:py-5">
        <RouterView v-slot="{ Component }">
          <Transition name="fade-slide" mode="out-in">
            <component :is="Component" :key="route.path" />
          </Transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>
