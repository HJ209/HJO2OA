export type MessageReadStatus = 'UNREAD' | 'READ'

export type MessageType = 'SYSTEM' | 'APPROVAL' | 'NOTICE' | 'TASK' | 'ALERT'

export interface MessageNotificationSummary {
  id: string
  type: MessageType
  title: string
  summary: string
  readStatus: MessageReadStatus
  createdAt: string
}

export interface MessageNotificationDetail {
  id: string
  type: MessageType
  title: string
  body: string
  readStatus: MessageReadStatus
  createdAt: string
  readAt?: string
  metadata?: Record<string, string | number | boolean | null>
}

export interface UnreadCount {
  count: number
}
