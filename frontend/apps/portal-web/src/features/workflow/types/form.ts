export type FormFieldType =
  | 'TEXT'
  | 'NUMBER'
  | 'DATE'
  | 'DATETIME'
  | 'SELECT'
  | 'MULTI_SELECT'
  | 'PERSON'
  | 'ORG'
  | 'DEPT'
  | 'POSITION'
  | 'ROLE'
  | 'ATTACHMENT'
  | 'RICH_TEXT'
  | 'IMAGE'
  | 'TABLE'
  | 'REFERENCE'

export interface FormFieldDefinition {
  fieldCode: string
  fieldName: string
  fieldNameI18nKey?: string
  fieldType: FormFieldType
  required?: boolean
  defaultValue?: unknown
  dictionaryCode?: string
  multiValue?: boolean
  visible?: boolean
  editable?: boolean
  maxLength?: number
  min?: number
  max?: number
  pattern?: string
  childFields?: FormFieldDefinition[]
  linkageRules?: unknown
}

export interface FormMetadataDetail {
  id: string
  code: string
  name: string
  nameI18nKey?: string
  version: number
  status: 'DRAFT' | 'PUBLISHED' | 'DEPRECATED'
  fieldSchema: FormFieldDefinition[]
  layout: unknown
  validations?: unknown
  fieldPermissionMap?: Record<string, Record<string, FieldPermission>>
  tenantId: string
  publishedAt?: string
  createdAt: string
  updatedAt: string
}

export interface FieldPermission {
  visible?: boolean
  editable?: boolean
  required?: boolean
}

export interface FormRenderSchema {
  metadataId: string
  code: string
  name: string
  nameI18nKey?: string
  version: number
  fieldSchema: FormFieldDefinition[]
  layout: unknown
  validations?: unknown
  fieldPermissionMap?: Record<string, Record<string, FieldPermission>>
  tenantId: string
  publishedAt?: string
}

export interface MetadataSnapshotRequest {
  metadataId: string
  code: string
  name: string
  version: number
  fields: FormFieldDefinition[]
  layout: unknown
  validations?: unknown[]
  fieldPermissionMap?: Record<string, Record<string, FieldPermission>>
  tenantId: string
}

export interface RenderedField {
  fieldCode: string
  fieldName: string
  displayName: string
  fieldType: FormFieldType
  value?: unknown
  dictionaryCode?: string
  multiValue: boolean
  visible: boolean
  editable: boolean
  required: boolean
  maxLength?: number
  min?: number
  max?: number
  pattern?: string
  childFields: RenderedField[]
  linkageRules?: unknown
}

export interface ValidationError {
  fieldCode: string
  message: string
}

export interface FormValidationResult {
  valid: boolean
  errors: ValidationError[]
}

export interface RenderedForm {
  metadataId: string
  code: string
  name: string
  displayName: string
  version: number
  nodeId?: string
  locale: string
  processInstanceId?: string
  formDataId?: string
  layout: unknown
  fields: RenderedField[]
  validation: FormValidationResult
}

export interface FormSubmission {
  submissionId: string
  metadataId: string
  metadataCode: string
  metadataVersion: number
  processInstanceId?: string
  formDataId?: string
  nodeId?: string
  status: 'DRAFT' | 'SUBMITTED'
  formData: Record<string, unknown>
  attachmentIds: string[]
  validation: FormValidationResult
  submittedBy: string
  tenantId: string
  createdAt: string
  updatedAt: string
  submittedAt?: string
}
