package com.varabyte.kobweb.site.pages

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.site.components.layouts.PageLayoutData

@InitRoute
fun initBillard3DPage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Billard3D", "Vertical two-thirds table trainer with top and player perspective views."))
}

@Page
@Composable
@Layout(".components.layouts.PageLayout")
fun Billard3DPage() {
    Billard3DScreen()
}
