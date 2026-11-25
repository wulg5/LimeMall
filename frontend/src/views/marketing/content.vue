<template>
  <div class="page-container">
    <div class="page-card">
      <div class="table-toolbar">
        <span class="title">内容管理</span>
        <el-button v-if="userStore.hasPerm('cms:content:edit')" type="primary" :icon="Plus"
                   @click="activeTab === 'banners' ? openBanner() : openNotice()">
          {{ activeTab === 'banners' ? '新增轮播' : '新增公告' }}
        </el-button>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="首页轮播" name="banners">
          <el-table v-loading="loading" :data="banners">
            <el-table-column label="标题" min-width="220">
              <template #default="{ row }">
                <div class="banner-cell">
                  <span class="banner-dot" :style="{ background: `linear-gradient(120deg, ${row.bgFrom}, ${row.bgTo})` }"></span>
                  <div>
                    <div>{{ row.tag }} · {{ row.title }}</div>
                    <div class="sub">{{ row.subtitle }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="linkUrl" label="跳转地址" min-width="180" show-overflow-tooltip />
            <el-table-column prop="sort" label="排序" width="80" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button v-if="userStore.hasPerm('cms:content:edit')" link type="primary"
                           @click="openBanner(row)">编辑</el-button>
                <el-button v-if="userStore.hasPerm('cms:content:edit')" link type="danger"
                           @click="removeBanner(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="商城公告" name="notices">
          <el-table v-loading="loading" :data="notices">
            <el-table-column prop="content" label="公告内容" min-width="320" />
            <el-table-column prop="sort" label="排序" width="80" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '显示' : '隐藏' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button v-if="userStore.hasPerm('cms:content:edit')" link type="primary"
                           @click="openNotice(row)">编辑</el-button>
                <el-button v-if="userStore.hasPerm('cms:content:edit')" link type="danger"
                           @click="removeNotice(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 轮播编辑弹窗 -->
    <el-dialog v-model="bannerVisible" :title="bannerForm.id ? '编辑轮播' : '新增轮播'" width="620px" destroy-on-close>
      <el-form ref="bannerFormRef" :model="bannerForm" :rules="bannerRules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="角标" prop="tag"><el-input v-model="bannerForm.tag" maxlength="32" /></el-form-item>
            <el-form-item label="主标题" prop="title"><el-input v-model="bannerForm.title" maxlength="100" /></el-form-item>
            <el-form-item label="副标题"><el-input v-model="bannerForm.subtitle" maxlength="255" /></el-form-item>
            <el-form-item label="按钮文字"><el-input v-model="bannerForm.ctaText" maxlength="32" /></el-form-item>
            <el-form-item label="跳转地址" prop="linkUrl"><el-input v-model="bannerForm.linkUrl" placeholder="/shop/products" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="渐变起始色" prop="bgFrom"><el-color-picker v-model="bannerForm.bgFrom" /></el-form-item>
            <el-form-item label="渐变结束色" prop="bgTo"><el-color-picker v-model="bannerForm.bgTo" /></el-form-item>
            <el-form-item label="拼贴图1"><el-input v-model="bannerForm.image1" placeholder="/img/products/p02.svg" /></el-form-item>
            <el-form-item label="拼贴图2"><el-input v-model="bannerForm.image2" placeholder="/img/products/p03.svg" /></el-form-item>
            <el-form-item label="排序"><el-input-number v-model="bannerForm.sort" :min="0" /></el-form-item>
            <el-form-item label="状态">
              <el-radio-group v-model="bannerForm.status">
                <el-radio :value="1">上架</el-radio>
                <el-radio :value="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="bannerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBanner">保存</el-button>
      </template>
    </el-dialog>

    <!-- 公告编辑弹窗 -->
    <el-dialog v-model="noticeVisible" :title="noticeForm.id ? '编辑公告' : '新增公告'" width="480px" destroy-on-close>
      <el-form ref="noticeFormRef" :model="noticeForm" :rules="noticeRules" label-width="88px">
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="noticeForm.content" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="noticeForm.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="noticeForm.status">
            <el-radio :value="1">显示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveNotice">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  bannerCreate, bannerDelete, bannerList, bannerUpdate,
  noticeCreate, noticeDelete, noticeList, noticeUpdate
} from '@/api/content'

const userStore = useUserStore()

const activeTab = ref('banners')
const loading = ref(false)
const saving = ref(false)
const banners = ref([])
const notices = ref([])

const bannerVisible = ref(false)
const bannerFormRef = ref()
const bannerForm = reactive({})
const bannerRules = {
  title: [{ required: true, message: '请填写主标题', trigger: 'blur' }],
  linkUrl: [{ required: true, message: '请填写跳转地址', trigger: 'blur' }],
  bgFrom: [{ required: true, message: '请选择起始色', trigger: 'change' }],
  bgTo: [{ required: true, message: '请选择结束色', trigger: 'change' }]
}

const noticeVisible = ref(false)
const noticeFormRef = ref()
const noticeForm = reactive({})
const noticeRules = {
  content: [{ required: true, message: '请填写公告内容', trigger: 'blur' }]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    ;[banners.value, notices.value] = await Promise.all([bannerList(), noticeList()])
  } finally {
    loading.value = false
  }
}

function openBanner(row) {
  Object.keys(bannerForm).forEach((key) => delete bannerForm[key])
  Object.assign(bannerForm, row
    ? { ...row }
    : { tag: '', title: '', subtitle: '', ctaText: '立即查看', linkUrl: '/shop', bgFrom: '#0b3d33', bgTo: '#0d9488', image1: '', image2: '', sort: 0, status: 1 })
  bannerVisible.value = true
}

async function saveBanner() {
  await bannerFormRef.value.validate()
  saving.value = true
  try {
    await (bannerForm.id ? bannerUpdate({ ...bannerForm }) : bannerCreate({ ...bannerForm }))
    ElMessage.success('保存成功')
    bannerVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function removeBanner(row) {
  try {
    await ElMessageBox.confirm(`确定删除轮播「${row.title}」吗？`, '删除确认', { type: 'warning' })
    await bannerDelete(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 取消 */ }
}

function openNotice(row) {
  Object.keys(noticeForm).forEach((key) => delete noticeForm[key])
  Object.assign(noticeForm, row ? { ...row } : { content: '', sort: 0, status: 1 })
  noticeVisible.value = true
}

async function saveNotice() {
  await noticeFormRef.value.validate()
  saving.value = true
  try {
    await (noticeForm.id ? noticeUpdate({ ...noticeForm }) : noticeCreate({ ...noticeForm }))
    ElMessage.success('保存成功')
    noticeVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function removeNotice(row) {
  try {
    await ElMessageBox.confirm('确定删除该公告吗？', '删除确认', { type: 'warning' })
    await noticeDelete(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 取消 */ }
}
</script>

<style scoped>
.banner-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.banner-dot {
  width: 40px;
  height: 26px;
  border-radius: 6px;
  flex-shrink: 0;
}

.sub {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}
</style>
