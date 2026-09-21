package com.expensevault.feature.transactions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Returns a consistent Material Symbols outline icon based on category name or iconName.
 */
fun getCategoryOutlineIcon(name: String?, iconName: String? = null): ImageVector {
    val key = "${name.orEmpty()} ${iconName.orEmpty()}".lowercase()
    return when {
        key.contains("grocer") || key.contains("cart") -> Icons.Outlined.ShoppingCart
        key.contains("food") || key.contains("dining") || key.contains("restaurant") -> Icons.Outlined.Restaurant
        key.contains("delivery") || key.contains("coffee") || key.contains("cafe") || key.contains("snack") -> Icons.Outlined.LocalCafe
        key.contains("transport") || key.contains("fuel") || key.contains("car") || key.contains("transit") || key.contains("ride") || key.contains("cab") -> Icons.Outlined.DirectionsCar
        key.contains("bill") || key.contains("utilit") || key.contains("receipt") || key.contains("electric") || key.contains("water") || key.contains("gas") -> Icons.Outlined.ReceiptLong
        key.contains("housing") || key.contains("rent") || key.contains("home") || key.contains("house") -> Icons.Outlined.Home
        key.contains("shopping") || key.contains("cloth") || key.contains("electronics") -> Icons.Outlined.ShoppingBag
        key.contains("health") || key.contains("medic") || key.contains("pharm") || key.contains("fitness") || key.contains("gym") -> Icons.Outlined.LocalHospital
        key.contains("entertainment") || key.contains("movie") || key.contains("game") || key.contains("subscription") -> Icons.Outlined.Movie
        key.contains("education") || key.contains("book") || key.contains("course") || key.contains("school") -> Icons.Outlined.School
        key.contains("personal") || key.contains("gift") || key.contains("charity") -> Icons.Outlined.CardGiftcard
        key.contains("salary") || key.contains("income") || key.contains("pay") -> Icons.Outlined.Payments
        else -> Icons.Outlined.Category
    }
}
