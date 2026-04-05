package com.varabyte.kobweb.site.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.TextAlign
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.minHeight
import com.varabyte.kobweb.compose.ui.modifiers.textAlign
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.site.components.style.MutedSpanTextVariant
import com.varabyte.kobweb.site.components.style.SiteTextSize
import com.varabyte.kobweb.site.components.style.dividerBoxShadow
import com.varabyte.kobweb.site.components.style.siteText
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px

@Composable
fun Footer(modifier: Modifier = Modifier) {
    Box(
        Modifier.fillMaxWidth().minHeight(140.px).dividerBoxShadow().then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth(70.percent).margin(1.em),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            /*
            OssLabel()
            Logo()
            SpanText(
                "Copyright © 2025 Varabyte. All rights reserved.",
                Modifier.siteText(SiteTextSize.TINY).textAlign(TextAlign.Center),
                MutedSpanTextVariant
            )
            */
            TrainerBrand(iconSizePx = 24, textSizeRem = 0.95)
            SpanText(
                "Built for fractional overlap practice.",
                Modifier.siteText(SiteTextSize.TINY).textAlign(TextAlign.Center),
                MutedSpanTextVariant
            )
        }
    }
}
