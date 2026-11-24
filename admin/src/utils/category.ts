export interface CategoryNode {
  id: number
  parentId: number
  categoryName: string
  status: number
  sortOrder?: number
}

export type CategoryTree<T> = T & { children?: CategoryTree<T>[] }

export function categoryTree<T extends CategoryNode>(items: T[]): CategoryTree<T>[] {
  const nodes = new Map(items.map((item) => [item.id, { ...item } as CategoryTree<T>]))
  const roots: CategoryTree<T>[] = []
  for (const item of items) {
    const node = nodes.get(item.id)!
    const parent = nodes.get(item.parentId)
    if (parent && parent !== node) (parent.children ??= []).push(node)
    else roots.push(node)
  }
  const sort = (list: CategoryTree<T>[]) => {
    list.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.id - b.id)
    list.forEach((node) => { if (node.children) sort(node.children) })
  }
  sort(roots)
  return roots
}

// Disabled ancestors also make a category unavailable for new product assignments.
export function categoryEnabled(item: CategoryNode, items: CategoryNode[]): boolean {
  const visited = new Set<number>()
  let current: CategoryNode | undefined = item
  while (current) {
    if (current.status !== 1 || visited.has(current.id)) return false
    visited.add(current.id)
    if (!current.parentId) return true
    current = items.find((candidate) => candidate.id === current!.parentId)
  }
  return false
}
