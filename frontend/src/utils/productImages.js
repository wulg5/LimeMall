import demoPhotos from './demoProductPhotos.json'

export const PRODUCT_PLACEHOLDER = '/img/product-placeholder.svg'

export function isDemoProductPhoto(source) {
  return productImageSource(source).startsWith('/img/demo-products/')
}

/** Only replace the seed asset paths. Uploaded and remote product images still use their original URLs. */
export function productImageSource(source) {
  if (!source) return PRODUCT_PLACEHOLDER
  const seedImage = /^\/img\/products\/(p(?:0[1-9]|1\d|2\d))\.svg$/.exec(source)
  if (!seedImage) return source
  const path = '/img/demo-products/' + seedImage[1] + '.webp'
  return demoPhotos.includes(seedImage[1]) ? path : PRODUCT_PLACEHOLDER
}
