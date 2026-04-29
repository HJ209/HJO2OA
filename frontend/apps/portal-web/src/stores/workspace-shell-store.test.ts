import { beforeEach, describe, expect, it } from 'vitest'
import { useWorkspaceShellStore } from '@/stores/workspace-shell-store'

describe('workspace shell store', () => {
  beforeEach(() => {
    useWorkspaceShellStore.setState({
      activeKey: 'home',
      openKeys: ['home'],
    })
  })

  it('keeps the home window pinned when closing windows', () => {
    useWorkspaceShellStore.getState().openWindow('org')
    useWorkspaceShellStore.getState().closeWindow('home')

    expect(useWorkspaceShellStore.getState().openKeys).toEqual(['home', 'org'])

    useWorkspaceShellStore.getState().closeOtherWindows('org')

    expect(useWorkspaceShellStore.getState().openKeys).toEqual(['home', 'org'])

    useWorkspaceShellStore.getState().closeAllWindows()

    expect(useWorkspaceShellStore.getState().openKeys).toEqual(['home'])
  })
})
