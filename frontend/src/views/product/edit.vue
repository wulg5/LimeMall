<template>
  <div class="page-container">
    <div class="page-card">
      <div class="table-toolbar">
        <span class="title">{{ isEdit ? '编辑商品' : '新增商品' }}</span>
        <el-button @click="$router.push('/admin/product/list')">返回列表</el-button>
      </div>

      <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" maxlength="200" placeholder="请输入商品名称" />
            </el-form-item>
            <el-form-item label="副标题">
              <el-input v-model="form.subTitle" maxlength="255" placeholder="一句话卖点" />
            </el-form-item>
            <el-form-item label="商品分类" prop="categoryPath">
              <el-cascader v-model="form.categoryPath" :options="categoryOptions" :props="cascaderProps"
                           placeholder="请选择二级分类" style="width: 100%" @change="onCategoryChange" />
            </el-form-item>
            <el-form-item label="品牌">
              <el-select v-model="form.brandId" placeholder="请选择品牌" clearable style="width: 100%">
                <el-option v-for="brand in brands" :key="brand.id" :label="brand.name" :value="brand.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="划线原价">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :controls="false"
                               placeholder="展示划线价" style="width: 200px" />
            </el-form-item>
            <el-form-item label="单位">
              <el-input v-model="form.unit" style="width: 200px" placeholder="件 / 盒 / 双" />
            </el-form-item>
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" style="width: 200px" />
              <span class="form-tip">值越大越靠前</span>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="商品主图">
              <el-upload class="pic-uploader" :show-file-list="false" :http-request="uploadMainPic" accept="image/*">
                <img v-if="form.mainPic" :src="form.mainPic" class="uploaded-pic" alt="主图" />
                <div v-else class="upload-placeholder">
                  <el-icon><Plus /></el-icon>
                  <span>点击上传</span>
                </div>
              </el-upload>
              <span class="form-tip">支持 png / jpg / webp，建议 800×800</span>
            </el-form-item>
            <el-form-item label="上架状态">
              <el-radio-group v-model="form.publishStatus">
                <el-radio :value="1">上架</el-radio>
                <el-radio :value="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="商品详情">
              <el-input v-model="form.detailHtml" type="textarea" :rows="9"
                        placeholder="支持 HTML 富文本，如 <p>商品卖点...</p>" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">SKU 规格与库存</el-divider>

        <el-table :data="form.skus" size="small">
          <el-table-column label="规格（格式：颜色:云雾白;尺码:M）" min-width="280">
            <template #default="{ row }">
              <el-input v-model="row.specsText" placeholder="颜色:云雾白;尺码:M" />
            </template>
          </el-table-column>
          <el-table-column label="SKU 编码（留空自动生成）" width="220">
            <template #default="{ row }">
              <el-input v-model="row.skuCode" placeholder="自动生成" />
            </template>
          </el-table-column>
          <el-table-column label="价格（元）" width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.price" :min="0.01" :precision="2" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="库存" width="130">
            <template #default="{ row }">
              <el-input-number v-model="row.stock" :min="0" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="form.skus.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-button class="add-sku" :icon="Plus" @click="addSku">添加 SKU</el-button>

        <div class="form-footer">
          <el-button size="large" @click="$router.push('/admin/product/list')">取 消</el-button>
          <el-button type="primary" size="large" :loading="saving" @click="handleSave">保存商品</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { brandOptions, categoryTree, productCreate, productDetail, productUpdate } from '@/api/product'
import { specsToText, textToSpecs } from '@/utils/format'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const brands = ref([])
const categoryOptions = ref([])

const isEdit = computed(() => !!route.query.id)

const cascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children'
}

const form = reactive({
  id: null,
  name: '',
  subTitle: '',
  categoryPath: [],
  categoryId: null,
  brandId: null,
  mainPic: '',
  originalPrice: null,
  unit: '件',
  detailHtml: '',
  sort: 0,
  publishStatus: 1,
  skus: []
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryPath: [{ required: true, type: 'array', min: 2, message: '请选择二级分类', trigger: 'change' }]
}

onMounted(async () => {
  loading.value = true
  try {
    ;[brands.value, categoryOptions.value] = await Promise.all([brandOptions(), categoryTree()])
    if (isEdit.value) {
      await loadDetail()
    } else {
      addSku()
    }
  } finally {
    loading.value = false
  }
})

async function loadDetail() {
  const data = await productDetail(route.query.id)
  form.id = data.id
  form.name = data.name
  form.subTitle = data.subTitle
  form.categoryId = data.categoryId
  form.brandId = data.brandId
  form.mainPic = data.mainPic
  form.originalPrice = data.originalPrice
  form.unit = data.unit
  form.detailHtml = data.detailHtml
  form.sort = data.sort
  form.publishStatus = data.publishStatus
  form.skus = (data.skus || []).map((sku) => ({
    id: sku.id,
    skuCode: sku.skuCode,
    specsText: specsToText(sku.specs),
    price: Number(sku.price),
    stock: sku.stock,
    originalStock: sku.stock,
    pic: sku.pic
  }))
  // 回显分类路径：在树中查找该二级分类的父级
  const parent = categoryOptions.value.find((node) =>
    (node.children || []).some((child) => child.id === data.categoryId)
  )
  form.categoryPath = parent ? [parent.id, data.categoryId] : [data.categoryId]
}

function onCategoryChange(path) {
  form.categoryId = path && path.length ? path[path.length - 1] : null
}

function addSku() {
  form.skus.push({ id: null, skuCode: '', specsText: '', price: null, stock: 0, pic: '' })
}

async function uploadMainPic(options) {
  const data = new FormData()
  data.append('file', options.file)
  const url = await request.post('/admin/upload/image', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  form.mainPic = url
  ElMessage.success('图片上传成功')
}

async function handleSave() {
  await formRef.value.validate()
  if (!form.skus.length) {
    ElMessage.warning('请至少添加一个 SKU')
    return
  }
  for (const sku of form.skus) {
    if (!sku.specsText) {
      ElMessage.warning('请填写每个 SKU 的规格')
      return
    }
    if (!sku.price) {
      ElMessage.warning('请填写每个 SKU 的价格')
      return
    }
    if (!Number.isInteger(sku.stock) || sku.stock < 0) {
      ElMessage.warning('请填写每个 SKU 的有效库存')
      return
    }
  }
  saving.value = true
  try {
    const payload = {
      id: form.id,
      name: form.name,
      subTitle: form.subTitle,
      categoryId: form.categoryId,
      brandId: form.brandId ?? null,
      mainPic: form.mainPic,
      originalPrice: form.originalPrice ?? null,
      unit: form.unit,
      detailHtml: form.detailHtml,
      sort: form.sort,
      publishStatus: form.publishStatus,
      skus: form.skus.map((sku) => ({
        id: sku.id,
        skuCode: sku.skuCode || null,
        specs: textToSpecs(sku.specsText),
        price: sku.price,
        stock: !sku.id || sku.stock !== sku.originalStock ? sku.stock : undefined,
        expectedStock: sku.id && sku.stock !== sku.originalStock ? sku.originalStock : undefined,
        pic: sku.pic
      }))
    }
    if (isEdit.value) {
      await productUpdate(payload)
      ElMessage.success('商品已更新')
    } else {
      await productCreate(payload)
      ElMessage.success('商品创建成功')
    }
    router.push('/admin/product/list')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #9ca3af;
}

.pic-uploader :deep(.el-upload) {
  border: none;
  cursor: pointer;
}

.uploaded-pic {
  width: 128px;
  height: 128px;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid #e5e7eb;
}

.upload-placeholder {
  width: 128px;
  height: 128px;
  border: 1px dashed #d1d5db;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #9ca3af;
  font-size: 12px;
  transition: border-color 0.2s;
}

.upload-placeholder:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.add-sku {
  margin-top: 12px;
  width: 100%;
  border-style: dashed;
}

.form-footer {
  margin-top: 28px;
  display: flex;
  justify-content: center;
  gap: 16px;
}

.form-footer .el-button {
  min-width: 140px;
}
</style>
