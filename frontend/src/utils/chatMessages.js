export const CHAT_PAGE_SIZE = 100

export function mergeRecentMessages(current, recent) {
  const firstRecentId = recent[0]?.id
  const reset = !current.length || firstRecentId > current.at(-1)?.id
  return {
    reset,
    messages: reset ? recent : [...current.filter(message => firstRecentId == null || message.id < firstRecentId), ...recent]
  }
}
