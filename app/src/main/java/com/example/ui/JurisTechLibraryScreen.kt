package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RecentDocumentEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JurisTechLibraryScreen(
    recentDocuments: List<RecentDocumentEntity>,
    onSelectDocument: (RecentDocumentEntity) -> Unit,
    onAddDocument: () -> Unit
) {
    Scaffold(
        containerColor = JtBackground,
        bottomBar = {
            NavigationBar(
                containerColor = JtBackground,
                contentColor = JtPrimaryText,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Biblioteca") },
                    label = { Text("Biblioteca") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JtGreenPrimary,
                        selectedTextColor = JtGreenPrimary,
                        indicatorColor = JtBorder,
                        unselectedIconColor = JtSecondaryText,
                        unselectedTextColor = JtSecondaryText
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    label = { Text("Buscar") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = JtSecondaryText,
                        unselectedTextColor = JtSecondaryText
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = JtSecondaryText,
                        unselectedTextColor = JtSecondaryText
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Home", color = JtSecondaryText, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "JurisTech",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = JtPrimaryText,
                letterSpacing = (-1).sp
            )
            Text(
                text = "R E A D E R",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = JtPrimaryText,
                letterSpacing = 6.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tu biblioteca jurídica, siempre contigo",
                fontSize = 14.sp,
                color = JtSecondaryText
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (recentDocuments.isEmpty()) {
                EmptyLibraryView(Modifier.weight(1f), onAddDocument)
            } else {
                LibraryCarouselView(
                    modifier = Modifier.weight(1f),
                    documents = recentDocuments,
                    onSelectDocument = onSelectDocument,
                    onAddDocument = onAddDocument
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryView(modifier: Modifier = Modifier, onAddDocument: () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = null,
            tint = JtGold,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Añade tu primer documento para comenzar.",
            fontSize = 14.sp,
            color = JtSecondaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = onAddDocument,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = JtPrimaryText),
            border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = JtPrimaryText)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Añadir documento", fontSize = 16.sp)
        }
    }
}

@Composable
fun LibraryCarouselView(
    modifier: Modifier = Modifier,
    documents: List<RecentDocumentEntity>,
    onSelectDocument: (RecentDocumentEntity) -> Unit,
    onAddDocument: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { documents.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Carousel
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentPadding = PaddingValues(horizontal = 80.dp)
        ) { page ->
            val document = documents[page]
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp)
                    .graphicsLayer {
                        // 3D Tilt effect
                        rotationY = pageOffset * -35f
                        
                        val scale = 1f - (pageOffset.absoluteValue * 0.15f)
                        scaleX = scale
                        scaleY = scale
                        
                        alpha = 1f - (pageOffset.absoluteValue * 0.4f)
                        cameraDistance = 12 * density
                    }
                    .clickable {
                        if (pageOffset.absoluteValue < 0.1f) {
                            onSelectDocument(document)
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                BookCover(
                    title = document.title,
                    isCenter = pageOffset.absoluteValue < 0.1f
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Navigation arrows and counter
        Row(
            modifier = Modifier.fillMaxWidth(0.6f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { 
                    coroutineScope.launch {
                        if (pagerState.currentPage > 0) pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(JtGreenPrimary, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Anterior", tint = Color.White)
            }
            
            Text(
                text = "${pagerState.currentPage + 1} / ${documents.size}",
                color = JtSecondaryText,
                fontSize = 14.sp
            )
            
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        if (pagerState.currentPage < documents.size - 1) pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(JtGreenPrimary, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Siguiente", tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Selected document info
        val currentDoc = documents[pagerState.currentPage]
        Text(
            text = currentDoc.title,
            color = JtPrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Text(
            text = "República de Honduras", // Placeholder as per design, or use currentDoc.subtitle if available
            color = JtSecondaryText,
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress bar
        val totalPages = if (currentDoc.totalPages > 0) currentDoc.totalPages else 1
        val percent = ((currentDoc.lastPage.toFloat() / totalPages.toFloat()) * 100).toInt()
        
        Row(
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Última lectura: Página ${currentDoc.lastPage}", color = JtSecondaryText, fontSize = 12.sp)
            Text("$percent%", color = JtSecondaryText, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = JtGreenPrimary,
            trackColor = JtBorder
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Buttons
        Button(
            onClick = { onSelectDocument(currentDoc) },
            colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
        ) {
            Text("Continuar leyendo", color = Color.White, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onAddDocument,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = JtPrimaryText),
            border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = JtPrimaryText)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Añadir documento", fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BookCover(title: String, isCenter: Boolean) {
    val bgColor = if (isCenter) JtGreenPrimary else Color(0xFF8B8B83)
    val textColor = if (isCenter) JtGold else Color(0xFFDCDCDC)
    val spineColor = if (isCenter) JtGreenSecondary else Color(0xFF7A7A73)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = if (isCenter) 16.dp else 4.dp,
                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, topStart = 4.dp, bottomStart = 4.dp)
            )
            .background(bgColor, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, topStart = 4.dp, bottomStart = 4.dp))
    ) {
        // Spine shadow/crease
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(16.dp)
                .background(
                    color = spineColor,
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                )
                .align(Alignment.CenterStart)
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title.uppercase(),
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "República de Honduras",
                color = textColor,
                fontSize = 8.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
