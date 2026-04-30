export type ArticleStatus =
  | 'DRAFT'
  | 'REVIEWING'
  | 'PUBLISHED'
  | 'OFFLINE'
  | 'ARCHIVED'

export type CategoryStatus = 'ENABLED' | 'DISABLED'
export type ReviewMode = 'DIRECT' | 'REVIEW'
export type ScopeSubjectType =
  | 'ALL'
  | 'PERSON'
  | 'ASSIGNMENT'
  | 'POSITION'
  | 'DEPARTMENT'
  | 'ROLE'
export type ScopeEffect = 'ALLOW' | 'DENY'

export interface PermissionRuleInput {
  subjectType: ScopeSubjectType
  subjectId?: string | null
  effect: ScopeEffect
  scope?: 'READ' | 'MANAGE' | 'ALL'
  sortOrder?: number
}

export interface PublicationScopeRuleInput {
  subjectType: ScopeSubjectType
  subjectId?: string | null
  effect: ScopeEffect
  sortOrder?: number
}

export interface ContentCategory {
  id: string
  code: string
  name: string
  categoryType: string
  parentId?: string | null
  routePath?: string | null
  sortOrder: number
  visibleMode: string
  status: CategoryStatus
  versionNo: number
}

export interface ContentCategoryTreeNode {
  category: ContentCategory
  children: ContentCategoryTreeNode[]
}

export interface ContentAttachment {
  attachmentId: string
  fileName: string
  url: string
  contentType?: string
  size: number
}

export interface ContentVersion {
  id: string
  articleId: string
  tenantId: string
  versionNo: number
  title: string
  summary?: string
  bodyFormat: string
  bodyText: string
  bodyChecksum?: string
  coverAttachmentId?: string | null
  attachments: ContentAttachment[]
  tags: string[]
  editorId: string
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  sourceVersionNo?: number | null
  idempotencyKey?: string | null
  createdAt: string
  updatedAt: string
}

export interface ContentPublication {
  id: string
  articleId: string
  tenantId: string
  targetVersionNo: number
  reviewMode: ReviewMode
  reviewStatus: 'NOT_REQUIRED' | 'PENDING' | 'APPROVED' | 'REJECTED'
  publicationStatus:
    | 'DRAFT'
    | 'PENDING_REVIEW'
    | 'PUBLISHED'
    | 'OFFLINE'
    | 'ARCHIVED'
    | 'REJECTED'
  startAt?: string | null
  endAt?: string | null
  publishedAt?: string | null
  offlineAt?: string | null
  reason?: string | null
}

export interface ArticleSummary {
  id: string
  articleNo: string
  title: string
  summary?: string
  contentType: string
  mainCategoryId: string
  authorId: string
  authorName?: string
  status: ArticleStatus
  currentDraftVersionNo?: number | null
  currentPublishedVersionNo?: number | null
  createdAt: string
  updatedAt: string
}

export interface ArticleDetail {
  article: ArticleSummary
  currentVersion: ContentVersion
  publications: ContentPublication[]
}

export interface ArticlePage {
  items: ArticleSummary[]
  page: number
  size: number
  total: number
}

export interface UpsertCategoryInput {
  operatorId: string
  code: string
  name: string
  categoryType: string
  parentId?: string | null
  routePath?: string | null
  sortOrder: number
  visibleMode: string
  permissions?: PermissionRuleInput[]
}

export interface UpsertArticleInput {
  operatorId: string
  articleNo?: string
  title: string
  summary?: string
  bodyFormat: string
  bodyText: string
  coverAttachmentId?: string | null
  attachments: ContentAttachment[]
  tags: string[]
  contentType: string
  mainCategoryId: string
  authorId?: string
  authorName?: string
  sourceType: string
  sourceUrl?: string | null
}

export interface PublishArticleInput {
  operatorId: string
  versionNo?: number | null
  reviewMode: ReviewMode
  startAt?: string | null
  endAt?: string | null
  reason?: string
  scopes: PublicationScopeRuleInput[]
}

export interface ArticleListQuery {
  categoryId?: string
  status?: ArticleStatus
  authorId?: string
  keyword?: string
  page?: number
  size?: number
}
