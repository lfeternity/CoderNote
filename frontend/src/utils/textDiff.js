function normalizeLines(text) {
  const source = String(text || '').replace(/\r\n/g, '\n')
  return source.split('\n')
}

function buildSimpleRows(leftLines, rightLines) {
  const rows = []
  const maxLength = Math.max(leftLines.length, rightLines.length)
  let leftLineNo = 1
  let rightLineNo = 1

  for (let index = 0; index < maxLength; index += 1) {
    const left = leftLines[index]
    const right = rightLines[index]

    if (left !== undefined && right !== undefined && left === right) {
      rows.push({
        type: 'equal',
        leftLineNo: leftLineNo++,
        rightLineNo: rightLineNo++,
        leftText: left,
        rightText: right
      })
      continue
    }

    if (left !== undefined && right !== undefined) {
      rows.push({
        type: 'modified',
        leftLineNo: leftLineNo++,
        rightLineNo: rightLineNo++,
        leftText: left,
        rightText: right
      })
      continue
    }

    if (left !== undefined) {
      rows.push({
        type: 'removed',
        leftLineNo: leftLineNo++,
        rightLineNo: null,
        leftText: left,
        rightText: ''
      })
      continue
    }

    rows.push({
      type: 'added',
      leftLineNo: null,
      rightLineNo: rightLineNo++,
      leftText: '',
      rightText: right || ''
    })
  }

  return rows
}

function buildLcsMatrix(leftLines, rightLines) {
  const leftLength = leftLines.length
  const rightLength = rightLines.length
  const matrix = Array.from({ length: leftLength + 1 }, () => Array(rightLength + 1).fill(0))

  for (let leftIndex = 1; leftIndex <= leftLength; leftIndex += 1) {
    for (let rightIndex = 1; rightIndex <= rightLength; rightIndex += 1) {
      if (leftLines[leftIndex - 1] === rightLines[rightIndex - 1]) {
        matrix[leftIndex][rightIndex] = matrix[leftIndex - 1][rightIndex - 1] + 1
      } else {
        matrix[leftIndex][rightIndex] = Math.max(
          matrix[leftIndex - 1][rightIndex],
          matrix[leftIndex][rightIndex - 1]
        )
      }
    }
  }

  return matrix
}

function buildOperations(leftLines, rightLines, matrix) {
  let leftIndex = leftLines.length
  let rightIndex = rightLines.length
  const operations = []

  while (leftIndex > 0 && rightIndex > 0) {
    if (leftLines[leftIndex - 1] === rightLines[rightIndex - 1]) {
      operations.push({ type: 'equal', leftText: leftLines[leftIndex - 1], rightText: rightLines[rightIndex - 1] })
      leftIndex -= 1
      rightIndex -= 1
      continue
    }

    if (matrix[leftIndex - 1][rightIndex] >= matrix[leftIndex][rightIndex - 1]) {
      operations.push({ type: 'removed', leftText: leftLines[leftIndex - 1], rightText: '' })
      leftIndex -= 1
      continue
    }

    operations.push({ type: 'added', leftText: '', rightText: rightLines[rightIndex - 1] })
    rightIndex -= 1
  }

  while (leftIndex > 0) {
    operations.push({ type: 'removed', leftText: leftLines[leftIndex - 1], rightText: '' })
    leftIndex -= 1
  }

  while (rightIndex > 0) {
    operations.push({ type: 'added', leftText: '', rightText: rightLines[rightIndex - 1] })
    rightIndex -= 1
  }

  operations.reverse()
  return operations
}

function operationsToRows(operations) {
  const rows = []
  let leftLineNo = 1
  let rightLineNo = 1

  for (let index = 0; index < operations.length; index += 1) {
    const op = operations[index]
    if (op.type === 'equal') {
      rows.push({
        type: 'equal',
        leftLineNo: leftLineNo++,
        rightLineNo: rightLineNo++,
        leftText: op.leftText,
        rightText: op.rightText
      })
      continue
    }

    const chunk = []
    while (index < operations.length && operations[index].type !== 'equal') {
      chunk.push(operations[index])
      index += 1
    }
    index -= 1

    const removed = chunk.filter((item) => item.type === 'removed').map((item) => item.leftText)
    const added = chunk.filter((item) => item.type === 'added').map((item) => item.rightText)
    const pairLength = Math.max(removed.length, added.length)

    for (let pairIndex = 0; pairIndex < pairLength; pairIndex += 1) {
      const hasRemoved = pairIndex < removed.length
      const hasAdded = pairIndex < added.length
      const type = hasRemoved && hasAdded
        ? 'modified'
        : (hasRemoved ? 'removed' : 'added')
      rows.push({
        type,
        leftLineNo: hasRemoved ? leftLineNo++ : null,
        rightLineNo: hasAdded ? rightLineNo++ : null,
        leftText: hasRemoved ? removed[pairIndex] : '',
        rightText: hasAdded ? added[pairIndex] : ''
      })
    }
  }

  return rows
}

function buildFragments(rows) {
  const fragments = []
  let order = 1
  rows.forEach((row, rowIndex) => {
    if (row.type === 'equal') {
      return
    }
    const leftNo = row.leftLineNo ?? '-'
    const rightNo = row.rightLineNo ?? '-'
    fragments.push({
      order: order++,
      rowIndex,
      label: `#${order - 1} L${leftNo} / R${rightNo}`
    })
  })
  return fragments
}

export function buildSideBySideDiff(leftText, rightText) {
  const leftLines = normalizeLines(leftText)
  const rightLines = normalizeLines(rightText)
  const size = leftLines.length * rightLines.length
  const rows = size > 120000
    ? buildSimpleRows(leftLines, rightLines)
    : operationsToRows(buildOperations(leftLines, rightLines, buildLcsMatrix(leftLines, rightLines)))

  return {
    rows,
    fragments: buildFragments(rows)
  }
}
