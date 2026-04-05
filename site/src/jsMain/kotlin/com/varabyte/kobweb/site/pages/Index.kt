package com.varabyte.kobweb.site.pages

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.site.components.layouts.PageLayoutData

@InitRoute
fun initHomePage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Fractional Aiming Trainer", "Pool trainer for fractional overlap aiming practice."))
}

/*
@InitRoute
fun initHomePage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Home", "The official Kobweb site."))
}

@Page
@Composable
@Layout(".components.layouts.PageLayout")
fun HomePage() {
    Column(
        Modifier.width(100.percent),
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        HeroSection()
        FeaturesSection()
        CliSection()
        CtaSection()
    }
}
*/

@Page
@Composable
@Layout(".components.layouts.PageLayout")
fun HomePage() {
    PoolTrainerScreen()
}
