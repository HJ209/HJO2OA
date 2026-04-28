import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { SchedulerTriggerButton } from '@/features/infra-admin/components/scheduler-trigger-button'

describe('SchedulerTriggerButton', () => {
  it('triggers the selected task once', () => {
    const onTrigger = vi.fn()

    render(<SchedulerTriggerButton onTrigger={onTrigger} taskId="task-001" />)

    fireEvent.click(screen.getByRole('button', { name: /手动触发/ }))

    expect(onTrigger).toHaveBeenCalledTimes(1)
    expect(onTrigger).toHaveBeenCalledWith('task-001')
  })

  it('disables duplicate clicks while loading', () => {
    const onTrigger = vi.fn()

    render(
      <SchedulerTriggerButton
        isLoading
        onTrigger={onTrigger}
        taskId="task-001"
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: /触发中/ }))

    expect(onTrigger).not.toHaveBeenCalled()
  })
})
