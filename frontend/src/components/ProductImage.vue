<template>
  <img :src="resolvedSource" :alt="alt" :loading="eager ? 'eager' : 'lazy'" decoding="async" @error="usePlaceholder" />
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { productImageSource, PRODUCT_PLACEHOLDER } from '@/utils/productImages'

const props = defineProps({
  src: { type: String, default: '' },
  alt: { type: String, default: '' },
  eager: Boolean
})

const failed = ref(false)
const resolvedSource = computed(() => failed.value ? PRODUCT_PLACEHOLDER : productImageSource(props.src))

watch(() => props.src, () => { failed.value = false })

function usePlaceholder() {
  failed.value = true
}
</script>
