<template>
  <div class="page-container dashboard">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16">
      <el-col v-for="card in statCards" :key="card.label" :span="6">
        <div class="stat-card">
          <div class="icon-wrap" :style="{ background: card.bg }">
            <el-icon><component :is="card.icon" /></el-icon>
          </div>
          <div>
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value">{{ card.value }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 待办提醒 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <div class="todo-banner" @click="$router.push('/order/list?status=1')">
          <el-icon class="todo-icon" color="#d97706"><Van /></el-icon>
          <span>待发货订单</span>
          <b class="todo-num">{{ summary.pendingDeliverCount ?? 0 }}</b>
          <span class="todo-link">去处理 &gt;</span>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="todo-banner" @click="$router.push('/order/return')">
          <el-icon class="todo-icon" color="#dc2626"><RefreshLeft /></el-icon>
          <span>待审核退款</span>
          <b class="todo-num">{{ summary.pendingReturnCount ?? 0 }}</b>
          <span class="todo-link">去处理 &gt;</span>
        </div>
      </el-col>
    </el-row>

    <!-- 趋势与订单状态 -->
    <el-row :gutter="16">
      <el-col :span="16">
        <div class="page-card">
          <div class="table-toolbar">
            <span class="title">销售趋势</span>
            <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
              <el-radio-button :value="7">近 7 天</el-radio-button>
              <el-radio-button :value="30">近 30 天</el-radio-button>
              <el-radio-button :value="90">近 90 天</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="trendRef" class="chart" style="height: 320px"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="page-card">
          <div class="table-toolbar"><span class="title">订单状态分布</span></div>
          <div ref="statusRef" class="chart" style="height: 320px"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 分类占比与热销排行 -->
    <el-row :gutter="16">
      <el-col :span="9">
        <div class="page-card">
          <div class="table-toolbar"><span class="title">分类销售占比</span></div>
          <div ref="categoryRef" class="chart" style="height: 300px"></div>
        </div>
      </el-col>
      <el-col :span="15">
        <div class="page-card">
          <div class="table-toolbar"><span class="title">热销商品 TOP 10</span></div>
          <div ref="topRef" class="chart" style="height: 300px"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 最新订单 -->
    <div class="page-card">
      <div class="table-toolbar">
        <span class="title">最新订单</span>
        <el-button link type="primary" @click="$router.push('/order/list')">查看全部</el-button>
      </div>
      <el-table :data="latestOrders">
        <el-table-column prop="orderSn" label="订单号" min-width="190" />
        <el-table-column prop="nickname" label="会员" width="140" />
        <el-table-column label="实付金额" width="130">
          <template #default="{ row }">{{ money(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="ORDER_STATUS[row.status]?.type">{{ ORDER_STATUS[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="190" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import {
  getSummary, getTrend, getCategorySales, getTopProducts,
  getOrderStatus, getLatestOrders
} from '@/api/dashboard'
import { money, ORDER_STATUS } from '@/utils/format'

const summary = ref({})
const latestOrders = ref([])
const trendDays = ref(30)

const trendRef = ref()
const statusRef = ref()
const categoryRef = ref()
const topRef = ref()

let trendChart = null
let statusChart = null
let categoryChart = null
let topChart = null

const COLOR = '#10b981'
const PALETTE = ['#10b981', '#34d399', '#6ee7b7', '#06b6d4', '#0ea5e9', '#a3e635', '#fbbf24', '#f472b6']

const statCards = computed(() => [
  {
    label: '今日订单数',
    value: summary.value.todayOrderCount ?? '-',
    icon: 'Odometer',
    bg: 'linear-gradient(135deg, #34d399, #059669)'
  },
  {
    label: '今日成交金额',
    value: money(summary.value.todayPayAmount ?? 0),
    icon: 'Coin',
    bg: 'linear-gradient(135deg, #fbbf24, #d97706)'
  },
  {
    label: '会员总数',
    value: summary.value.memberCount ?? '-',
    icon: 'User',
    bg: 'linear-gradient(135deg, #60a5fa, #2563eb)'
  },
  {
    label: '在售商品',
    value: summary.value.productCount ?? '-',
    icon: 'Goods',
    bg: 'linear-gradient(135deg, #a78bfa, #7c3aed)'
  }
])

onMounted(async () => {
  initCharts()
  await Promise.all([loadSummary(), loadTrend(), loadStatus(), loadCategory(), loadTop(), loadLatest()])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  ;[trendChart, statusChart, categoryChart, topChart].forEach((chart) => chart && chart.dispose())
})

function handleResize() {
  ;[trendChart, statusChart, categoryChart, topChart].forEach((chart) => chart && chart.resize())
}

function initCharts() {
  trendChart = echarts.init(trendRef.value)
  statusChart = echarts.init(statusRef.value)
  categoryChart = echarts.init(categoryRef.value)
  topChart = echarts.init(topRef.value)
}

async function loadSummary() {
  summary.value = await getSummary()
}

async function loadTrend() {
  const data = await getTrend(trendDays.value)
  trendChart.setOption({
    color: [COLOR, '#fbbf24'],
    tooltip: { trigger: 'axis' },
    legend: { data: ['成交金额', '订单数'], top: 0 },
    grid: { left: 10, right: 10, top: 40, bottom: 0, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map((item) => item.date.slice(5)),
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#9ca3af' }
    },
    yAxis: [
      {
        type: 'value',
        name: '金额',
        axisLabel: { color: '#9ca3af' },
        splitLine: { lineStyle: { color: '#f3f4f6' } }
      },
      {
        type: 'value',
        name: '订单',
        axisLabel: { color: '#9ca3af' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '成交金额',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: data.map((item) => item.payAmount),
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16,185,129,0.28)' },
            { offset: 1, color: 'rgba(16,185,129,0.02)' }
          ])
        },
        lineStyle: { width: 3 }
      },
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        symbolSize: 6,
        data: data.map((item) => item.orderCount),
        lineStyle: { width: 2, type: 'dashed' }
      }
    ]
  })
}

async function loadStatus() {
  const data = await getOrderStatus()
  statusChart.setOption({
    color: PALETTE,
    tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)' },
    legend: { bottom: 0, icon: 'circle' },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '44%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{c}' },
        data: data.map((item) => ({
          name: ORDER_STATUS[item.status]?.text || `状态${item.status}`,
          value: item.count
        }))
      }
    ]
  })
}

async function loadCategory() {
  const data = await getCategorySales()
  categoryChart.setOption({
    color: PALETTE,
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 0, icon: 'circle' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '68%'],
        center: ['50%', '44%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{d}%' },
        data: data.map((item) => ({ name: item.name, value: item.amount }))
      }
    ]
  })
}

async function loadTop() {
  const data = await getTopProducts(10)
  topChart.setOption({
    color: [COLOR],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 30, top: 10, bottom: 0, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#f3f4f6' } }, axisLabel: { color: '#9ca3af' } },
    yAxis: {
      type: 'category',
      inverse: true,
      data: data.map((item) => item.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#4b5563',
        width: 180,
        overflow: 'truncate'
      }
    },
    series: [
      {
        type: 'bar',
        data: data.map((item) => item.quantity),
        barWidth: 14,
        itemStyle: {
          borderRadius: [0, 7, 7, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#6ee7b7' },
            { offset: 1, color: '#059669' }
          ])
        },
        label: { show: true, position: 'right', color: '#6b7280' }
      }
    ]
  })
}

async function loadLatest() {
  latestOrders.value = await getLatestOrders(8)
}
</script>

<style scoped>
.todo-banner {
  background: #ffffff;
  border-radius: 12px;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(15, 46, 36, 0.05);
  transition: transform 0.2s;
  color: #4b5563;
}

.todo-banner:hover {
  transform: translateY(-2px);
}

.todo-icon {
  font-size: 20px;
}

.todo-num {
  font-size: 20px;
  color: #111827;
  margin-left: 4px;
}

.todo-link {
  margin-left: auto;
  color: var(--el-color-primary);
  font-size: 13px;
}

.chart {
  width: 100%;
}
</style>
