import type { ReactElement, ReactNode } from 'react'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

export interface InfraPageSectionProps {
  title: string
  description: string
  actions?: ReactNode
  children: ReactNode
}

export function InfraPageSection({
  title,
  description,
  actions,
  children,
}: InfraPageSectionProps): ReactElement {
  return (
    <Card className="rounded-2xl">
      <CardHeader className="flex-row items-start justify-between gap-4">
        <div>
          <CardTitle>{title}</CardTitle>
          <CardDescription className="mt-1">{description}</CardDescription>
        </div>
        {actions}
      </CardHeader>
      <CardContent>{children}</CardContent>
    </Card>
  )
}
