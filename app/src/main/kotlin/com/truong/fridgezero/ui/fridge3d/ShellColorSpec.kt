package com.truong.fridgezero.ui.fridge3d

import androidx.compose.ui.graphics.Color

/** spec.md §1.4 palette with balanced matte-satin studio PBR values to prevent specular glare. */
data class ShellColorSpec(
  val key: String,
  val color: Color,
  val roughness: Float,
  val metalness: Float
)

object ShellColors {
  val Inox = ShellColorSpec("inox", Color(0xFFCBD5E1), 0.42f, 0.25f)
  val Butter = ShellColorSpec("butter", Color(0xFFFEF08A), 0.44f, 0.16f)
  val Mint = ShellColorSpec("mint", Color(0xFFA7F3D0), 0.44f, 0.16f)
  val Matte = ShellColorSpec("matte", Color(0xFF27272A), 0.56f, 0.22f)

  val all = listOf(Inox, Butter, Mint, Matte)

  /** Normalizes legacy color keys (such as "matte_black") to canonical spec keys. */
  fun normalizeKey(key: String): String = when (key.trim().lowercase()) {
    "butter" -> Butter.key
    "mint" -> Mint.key
    "matte", "matte_black" -> Matte.key
    else -> Inox.key
  }

  fun forKey(key: String): ShellColorSpec = when (normalizeKey(key)) {
    Butter.key -> Butter
    Mint.key -> Mint
    Matte.key -> Matte
    else -> Inox
  }
}
