<template>
  <view class="content">
    <image class="logo" src="/static/logo.png" />
    <view class="text-area">
      <text class="title">{{ title }}</text>
    </view>
    <button class="check" :loading="loading" @click="checkBackend">检查后端连通性</button>
    <text class="result">{{ result }}</text>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ping } from '@/api/ping'

const title = ref('食见 FoodView')
const loading = ref(false)
const result = ref('尚未检查')

async function checkBackend() {
  loading.value = true
  result.value = '请求中'
  try {
    const data = await ping()
    result.value = `后端返回：${data.status}`
  } catch (error) {
    result.value = `失败：${(error as Error).message}`
  } finally {
    loading.value = false
  }
}
</script>

<style>
.content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.logo {
  height: 200rpx;
  width: 200rpx;
  margin-top: 200rpx;
  margin-left: auto;
  margin-right: auto;
  margin-bottom: 50rpx;
}

.text-area {
  display: flex;
  justify-content: center;
}

.title {
  font-size: 36rpx;
  color: #8f8f94;
}

.check {
  margin-top: 60rpx;
  font-size: 28rpx;
}

.result {
  margin-top: 30rpx;
  font-size: 26rpx;
  color: #8f8f94;
}
</style>
