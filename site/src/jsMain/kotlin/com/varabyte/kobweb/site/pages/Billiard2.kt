package com.varabyte.kobweb.site.pages

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.site.components.layouts.PageLayoutData

@InitRoute
fun initBilliard2Page(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Billiard2", "Vertical two-thirds table trainer with aligned ball positions and visible side pockets."))
}

@Page
@Composable
@Layout(".components.layouts.PageLayout")
fun Billiard2Page() {
    Billiard2Screen()
}
