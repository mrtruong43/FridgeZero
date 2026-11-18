package com.truong.fridgezero.ui.designer

import com.truong.fridgezero.data.CompartmentEntity
import com.truong.fridgezero.data.CompartmentKind

const val MAX_COMPARTMENTS = 6

private const val MARGIN_X = 0.04f
private const val FULL_W = 0.92f
private const val TOP = 0.02f
private const val TOTAL_H = 0.96f
private const val GAP = 0.013f

/**
 * Recomputes normalized x, y, w, h for [list] so compartments never overlap, whatever the
 * add/remove/kind changes. Side-by-side puts the first compartment in a full-height left column and
 * stacks the rest on the right; every other template stacks vertically (drawers get less height).
 */
fun normalizeCompartmentLayout(
  list: List<CompartmentEntity>,
  template: String
): List<CompartmentEntity> {
  if (list.isEmpty()) return list

  fun stack(items: List<CompartmentEntity>, x: Float, w: Float): List<CompartmentEntity> {
    val weights = items.map { if (it.kind == CompartmentKind.DRAWER) 0.65f else 1f }
    val avail = TOTAL_H - GAP * (items.size - 1)
    val sum = weights.sum()
    var y = TOP
    return items.mapIndexed { i, c ->
      val h = avail * weights[i] / sum
      c.copy(x = x, y = y, w = w, h = h).also { y += h + GAP }
    }
  }

  val placed = if (template == "side_by_side" && list.size >= 2) {
    val left = list.first().copy(x = MARGIN_X, y = TOP, w = 0.41f, h = TOTAL_H)
    listOf(left) + stack(list.drop(1), 0.465f, 0.495f)
  } else {
    stack(list, MARGIN_X, FULL_W)
  }
  return placed.mapIndexed { index, c -> c.copy(sort = index) }
}
