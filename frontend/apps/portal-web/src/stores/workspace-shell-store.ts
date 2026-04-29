import { create } from 'zustand'
import {
  getDefaultWorkspacePath,
  type WorkspaceAppKey,
  resolveWorkspaceAppKey,
} from '@/features/workspace/workspace-registry'

const DEFAULT_WORKSPACE_KEY: WorkspaceAppKey = 'home'

interface WorkspaceShellState {
  activeKey: WorkspaceAppKey
  lastPaths: Record<WorkspaceAppKey, string>
  openKeys: WorkspaceAppKey[]
  activateWindow: (key: WorkspaceAppKey) => void
  closeAllWindows: () => void
  closeOtherWindows: (key: WorkspaceAppKey) => void
  closeWindow: (key: WorkspaceAppKey) => void
  openWindow: (key: WorkspaceAppKey) => void
  syncFromPath: (pathname: string) => void
}

function ensureWindowKeys(
  openKeys: WorkspaceAppKey[],
  key: WorkspaceAppKey,
): WorkspaceAppKey[] {
  if (openKeys.includes(key)) {
    return openKeys
  }

  return [...openKeys, key]
}

function createDefaultLastPaths(): Record<WorkspaceAppKey, string> {
  return {
    home: getDefaultWorkspacePath('home'),
    messages: getDefaultWorkspacePath('messages'),
    docs: getDefaultWorkspacePath('docs'),
    todo: getDefaultWorkspacePath('todo'),
    org: getDefaultWorkspacePath('org'),
    admin: getDefaultWorkspacePath('admin'),
  }
}

export const useWorkspaceShellStore = create<WorkspaceShellState>((set) => ({
  activeKey: DEFAULT_WORKSPACE_KEY,
  lastPaths: createDefaultLastPaths(),
  openKeys: [DEFAULT_WORKSPACE_KEY],
  activateWindow: (activeKey) => {
    set((state) => ({
      activeKey,
      openKeys: ensureWindowKeys(state.openKeys, activeKey),
    }))
  },
  closeAllWindows: () => {
    set({
      activeKey: DEFAULT_WORKSPACE_KEY,
      openKeys: [DEFAULT_WORKSPACE_KEY],
    })
  },
  closeOtherWindows: (key) => {
    set({
      activeKey: key,
      openKeys:
        key === DEFAULT_WORKSPACE_KEY
          ? [DEFAULT_WORKSPACE_KEY]
          : [DEFAULT_WORKSPACE_KEY, key],
    })
  },
  closeWindow: (key) => {
    set((state) => {
      if (key === DEFAULT_WORKSPACE_KEY) {
        return state
      }

      if (!state.openKeys.includes(key)) {
        return state
      }

      const nextOpenKeys = state.openKeys.filter((item) => item !== key)

      if (!nextOpenKeys.length) {
        return {
          activeKey: DEFAULT_WORKSPACE_KEY,
          openKeys: [DEFAULT_WORKSPACE_KEY],
        }
      }

      if (state.activeKey !== key) {
        return {
          activeKey: state.activeKey,
          openKeys: nextOpenKeys,
        }
      }

      const closedIndex = state.openKeys.indexOf(key)
      const fallbackKey =
        nextOpenKeys[closedIndex - 1] ??
        nextOpenKeys[closedIndex] ??
        DEFAULT_WORKSPACE_KEY

      return {
        activeKey: fallbackKey,
        openKeys: nextOpenKeys,
      }
    })
  },
  openWindow: (key) => {
    set((state) => ({
      activeKey: key,
      openKeys: ensureWindowKeys(state.openKeys, key),
    }))
  },
  syncFromPath: (pathname) => {
    const key = resolveWorkspaceAppKey(pathname)

    set((state) => ({
      activeKey: key,
      lastPaths: {
        ...state.lastPaths,
        [key]: pathname,
      },
      openKeys:
        key === DEFAULT_WORKSPACE_KEY
          ? ensureWindowKeys(state.openKeys, key)
          : ensureWindowKeys(
              ensureWindowKeys(state.openKeys, DEFAULT_WORKSPACE_KEY),
              key,
            ),
    }))
  },
}))
