package com.varabyte.kobweb.site.pages

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.site.components.layouts.PageLayoutData

@InitRoute
fun initBillard2Page(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Billard2", "Vertical two-thirds table trainer with only the upper corner pockets in play."))
}

@Page
@Composable
@Layout(".components.layouts.PageLayout")
fun Billard2Page() {
    Billard2Screen()
}
