export function skuPrice(sku) {
  return Number(sku.flashPrice ?? sku.price)
}

export function cheapestAvailableSku(skus) {
  return skus
    .filter(sku => sku.stock > 0)
    .sort((a, b) => skuPrice(a) - skuPrice(b) || Number(!a.flashPrice) - Number(!b.flashPrice) || a.id - b.id)[0]
}
