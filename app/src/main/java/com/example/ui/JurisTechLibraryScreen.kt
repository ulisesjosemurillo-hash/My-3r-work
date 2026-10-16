package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.RecentDocumentEntity
import com.example.model.SampleDocumentRepository
import com.example.model.SearchResultItem
import com.example.ui.theme.*
import com.example.viewmodel.JurisTechUiState
import com.example.viewmodel.JurisTechViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JurisTechLibraryScreen(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onSelectDocument: (RecentDocumentEntity) -> Unit,
    onAddFromFile: () -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showPasteTextDialog by remember { mutableStateOf(false) }
    var showCatalogDialog by remember { mutableStateOf(false) }
    var documentToRename by remember { mutableStateOf<RecentDocumentEntity?>(null) }
    var documentToChangeCover by remember { mutableStateOf<RecentDocumentEntity?>(null) }
    var documentToEditMetadata by remember { mutableStateOf<RecentDocumentEntity?>(null) }
    var showNewCollectionDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && documentToChangeCover != null) {
            viewModel.cambiarCaratulaDocumento(documentToChangeCover!!.id, uri.toString(), null)
            documentToChangeCover = null
        }
    }

    Scaffold(
        containerColor = JtBackground,
        floatingActionButton = {
            if (uiState.currentTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Añadir libro") },
                    text = { Text("Añadir Libro", fontWeight = FontWeight.Bold) },
                    containerColor = JtGreenPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_book")
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            NavigationBar(
                containerColor = JtBackground,
                contentColor = JtPrimaryText,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == 0,
                    onClick = { viewModel.setCurrentTab(0) },
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
                    selected = uiState.currentTab == 1,
                    onClick = { viewModel.setCurrentTab(1) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    label = { Text("Buscar") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JtGreenPrimary,
                        selectedTextColor = JtGreenPrimary,
                        indicatorColor = JtBorder,
                        unselectedIconColor = JtSecondaryText,
                        unselectedTextColor = JtSecondaryText
                    )
                )
                NavigationBarItem(
                    selected = uiState.currentTab == 2,
                    onClick = { viewModel.setCurrentTab(2) },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JtGreenPrimary,
                        selectedTextColor = JtGreenPrimary,
                        indicatorColor = JtBorder,
                        unselectedIconColor = JtSecondaryText,
                        unselectedTextColor = JtSecondaryText
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.currentTab) {
                0 -> {
                    LibraryHomeContent(
                        documents = uiState.recentDocuments,
                        uiState = uiState,
                        viewModel = viewModel,
                        onSelectDocument = onSelectDocument,
                        onAddDocument = { showAddDialog = true },
                        onRenameDocument = { documentToRename = it },
                        onChangeCover = { documentToChangeCover = it },
                        onEditMetadata = { documentToEditMetadata = it },
                        onShareDocument = { viewModel.compartirDocumento(context, it) },
                        onDeleteDocument = { viewModel.eliminarDocumentoReciente(it.id) },
                        onAddNewCollection = { showNewCollectionDialog = true }
                    )
                }
                1 -> {
                    LibrarySearchContent(
                        uiState = uiState,
                        onSearch = { viewModel.buscarEnBiblioteca(it) },
                        onSelectResult = { result ->
                            val doc = uiState.recentDocuments.firstOrNull { it.id == result.documentId }
                            if (doc != null) {
                                viewModel.retomarDocumento(doc)
                                viewModel.saltarAFragmento(result.fragmentIndex)
                            }
                        }
                    )
                }
                2 -> {
                    LibrarySettingsContent(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Modal para Añadir documento
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    "Añadir Libros a la Biblioteca",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = JtPrimaryText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Selecciona cómo deseas incorporar más libros y textos legales:",
                        color = JtSecondaryText,
                        fontSize = 13.sp
                    )

                    // 1. Instalar Colección Completa
                    FilledTonalButton(
                        onClick = {
                            viewModel.agregarTodosLosLibrosDelCatalogo()
                            showAddDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = JtGreenPrimary.copy(alpha = 0.15f),
                            contentColor = JtGreenPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LibraryAdd, contentDescription = null, tint = JtGreenPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Añadir Colección Completa (10 Libros)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Instala todos los códigos: Penal, Civil, Comercio, Tránsito, Trabajo...", color = JtSecondaryText, fontSize = 11.sp)
                        }
                    }

                    // 2. Explorar Catálogo de Libros
                    OutlinedButton(
                        onClick = {
                            showAddDialog = false
                            showCatalogDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JtPrimaryText),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = JtGreenPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Explorar Catálogo de Libros y Leyes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Elige y añade libros individualmente a tu estante", color = JtSecondaryText, fontSize = 11.sp)
                        }
                    }

                    // 3. Crear Libro con Plantilla o Escribir
                    OutlinedButton(
                        onClick = {
                            showAddDialog = false
                            showPasteTextDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JtPrimaryText),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = JtGreenPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Crear Libro con Plantilla o Texto", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Redacta un nuevo libro o usa plantillas prediseñadas", color = JtSecondaryText, fontSize = 11.sp)
                        }
                    }

                    // 4. Importar Archivo
                    OutlinedButton(
                        onClick = {
                            showAddDialog = false
                            onAddFromFile()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JtPrimaryText),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JtBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = JtGreenPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Importar PDF, Word (.docx) o TXT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Carga archivos almacenados en la memoria del dispositivo", color = JtSecondaryText, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cerrar", color = JtSecondaryText)
                }
            },
            containerColor = JtSurface
        )
    }

    // Modal Escribir / Crear Nuevo Libro
    if (showPasteTextDialog) {
        var pasteTitle by remember { mutableStateOf("") }
        var pasteAuthor by remember { mutableStateOf("JurisTech / Autor") }
        var pasteMateria by remember { mutableStateOf("Derecho General") }
        var pasteContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPasteTextDialog = false },
            title = {
                Text("Crear o Añadir Nuevo Libro", fontWeight = FontWeight.Bold, color = JtPrimaryText)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Plantillas rápidas:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = JtSecondaryText
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                pasteTitle = "Manual de Procedimientos y Juicios Orales"
                                pasteAuthor = "Dr. Roberto Mendoza"
                                pasteMateria = "Derecho Procesal Penal"
                                pasteContent = """CAPÍTULO I: TEORÍA DEL CASO Y LITIGACIÓN ORAL

ARTÍCULO 1.- La Teoría del Caso es el instrumento metodológico fundamental que estructura la hipótesis fáctica, jurídica y probatoria que la parte sostiene ante el tribunal de sentencia.

ARTÍCULO 2.- Elementos Estructurales:
1. Elemento Fáctico: Reconstrucción circunstanciada de los hechos relevantes acaecidos en tiempo, modo y lugar.
2. Elemento Jurídico: Encuadre dogmático de la conducta en los tipos penales y normas procesales aplicables.
3. Elemento Probatorio: Conjunto de medios probatorios idóneos, lícitos y pertinentes que sustentan cada afirmación de hecho.

CAPÍTULO II: TÉCNICAS DE INTERROGATORIO Y CONTRAINTERROGATORIO

ARTÍCULO 10.- El interrogatorio directo tiene como objetivo presentar el testimonio de los testigos propios de forma clara, ordenada y convincente, utilizando preguntas abiertas que permitan el relato fluido del declarante.

ARTÍCULO 15.- El contrainterrogatorio busca confrontar y poner a prueba la credibilidad del testigo adverso, empleando preguntas cerradas y sugestivas orientadas a evidenciar contradicciones, inconsistencias o falta de percepción directa."""
                            },
                            label = { Text("Manual Procesal", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(13.dp)) }
                        )

                        AssistChip(
                            onClick = {
                                pasteTitle = "Compendio de Derecho Civil y Contratos"
                                pasteAuthor = "Academia Jurídica"
                                pasteMateria = "Derecho Civil"
                                pasteContent = """TÍTULO I: DE LAS OBLIGACIONES Y RESPONSABILIDAD CIVIL

ARTÍCULO 1.- Principio de Autonomía de la Voluntad. Los particulares pueden estipular las cláusulas y condiciones que tengan por convenientes, siempre que no sean contrarias a las leyes, a la moral ni al orden público.

ARTÍCULO 2.- La Responsabilidad Civil Contractual y Extracontractual. Todo aquel que por acción u omisión cause daño a otro, interviniendo culpa o negligencia, está obligado a reparar el daño causado (principio neminem laedere).

TÍTULO II: DE LOS CONTRATOS TRASLATIVOS DE DOMINIO

ARTÍCULO 20.- Por el contrato de compraventa uno de los contratantes se obliga a entregar una cosa determinada y el otro a pagar por ella un precio cierto, en dinero o signo que lo represente."""
                            },
                            label = { Text("Contratos Civiles", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(13.dp)) }
                        )

                        AssistChip(
                            onClick = {
                                pasteTitle = "Tratado de Amparo y Garantías"
                                pasteAuthor = "Corte Suprema"
                                pasteMateria = "Derecho Constitucional"
                                pasteContent = """TÍTULO I: DE LA ACCIÓN DE AMPARO CONSTITUCIONAL

ARTÍCULO 1.- Naturaleza Jurídica. La Acción de Amparo es la garantía jurisdiccional de rango constitucional destinada a restablecer el goce de los derechos fundamentales conculcados o amenazados por actos u omisiones de cualquier autoridad o particular.

ARTÍCULO 5.- Requisitos de Procedibilidad:
a) Legitimación activa del agraviado;
b) Existencia de un acto lesivo de autoridad;
c) Agotamiento oportuno de los recursos ordinarios (principio de definitividad); y,
d) Presentación en el plazo perentorio de ley."""
                            },
                            label = { Text("Amparo Const.", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(13.dp)) }
                        )
                    }

                    OutlinedTextField(
                        value = pasteTitle,
                        onValueChange = { pasteTitle = it },
                        label = { Text("Título del libro / texto") },
                        placeholder = { Text("Ej. Código de Ética Judicial") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pasteAuthor,
                            onValueChange = { pasteAuthor = it },
                            label = { Text("Autor") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = pasteMateria,
                            onValueChange = { pasteMateria = it },
                            label = { Text("Materia") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = pasteContent,
                        onValueChange = { pasteContent = it },
                        label = { Text("Contenido del libro (artículos / capítulos)") },
                        placeholder = { Text("Escribe o pega aquí el contenido...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (pasteTitle.isNotBlank() && pasteContent.isNotBlank()) {
                                viewModel.crearNuevoLibroPersonalizado(
                                    titulo = pasteTitle.trim(),
                                    autor = pasteAuthor.trim(),
                                    materia = pasteMateria.trim(),
                                    contenido = pasteContent.trim(),
                                    abrirAhora = false
                                )
                                showPasteTextDialog = false
                            }
                        },
                        enabled = pasteTitle.isNotBlank() && pasteContent.isNotBlank()
                    ) {
                        Text("Añadir al Estante")
                    }

                    Button(
                        onClick = {
                            if (pasteTitle.isNotBlank() && pasteContent.isNotBlank()) {
                                viewModel.crearNuevoLibroPersonalizado(
                                    titulo = pasteTitle.trim(),
                                    autor = pasteAuthor.trim(),
                                    materia = pasteMateria.trim(),
                                    contenido = pasteContent.trim(),
                                    abrirAhora = true
                                )
                                showPasteTextDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                        enabled = pasteTitle.isNotBlank() && pasteContent.isNotBlank()
                    ) {
                        Text("Añadir y Leer", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteTextDialog = false }) {
                    Text("Cancelar", color = JtSecondaryText)
                }
            },
            containerColor = JtSurface
        )
    }

    // Modal Catálogo Completo de Libros y Leyes
    if (showCatalogDialog) {
        val existingTitles = remember(uiState.recentDocuments) {
            uiState.recentDocuments.map { it.title.trim().lowercase() }.toSet()
        }

        AlertDialog(
            onDismissRequest = { showCatalogDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Catálogo de Libros Jurídicos",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = JtPrimaryText,
                            fontSize = 18.sp
                        )
                        Text(
                            "${SampleDocumentRepository.hondurasSamples.size} libros disponibles",
                            fontSize = 12.sp,
                            color = JtSecondaryText
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón para añadir toda la colección
                    FilledTonalButton(
                        onClick = {
                            viewModel.agregarTodosLosLibrosDelCatalogo()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = JtGreenPrimary.copy(alpha = 0.12f),
                            contentColor = JtGreenPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.LibraryAddCheck, contentDescription = null, tint = JtGreenPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📥 Añadir Colección Completa (Todos)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    SampleDocumentRepository.hondurasSamples.forEach { sample ->
                        val isAlreadyAdded = existingTitles.contains(sample.title.trim().lowercase())

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = JtBackground),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isAlreadyAdded) JtGreenPrimary.copy(alpha = 0.4f) else JtBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Surface(
                                            color = if (sample.coverColor != null) Color(sample.coverColor) else JtGreenPrimary,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.MenuBook,
                                                    contentDescription = null,
                                                    tint = JtGold,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = sample.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = JtPrimaryText
                                            )
                                            Text(
                                                text = sample.subtitle,
                                                fontSize = 11.sp,
                                                color = JtSecondaryText,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    color = JtSurface,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = sample.materia,
                                                        fontSize = 10.sp,
                                                        color = JtGreenPrimary,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (sample.year.isNotBlank()) {
                                                    Surface(
                                                        color = JtSurface,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = sample.year,
                                                            fontSize = 10.sp,
                                                            color = JtSecondaryText,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (isAlreadyAdded) {
                                        Surface(
                                            color = JtGreenPrimary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "✓ En biblioteca",
                                                color = JtGreenPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Botones de acción del libro
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!isAlreadyAdded) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.agregarLibroALaBiblioteca(sample, abrirAhora = false)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = JtGreenPrimary),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, JtGreenPrimary),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Añadir a mi biblioteca", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.cargarSample(sample)
                                            showCatalogDialog = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Leer ahora", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCatalogDialog = false }) {
                    Text("Cerrar", color = JtSecondaryText)
                }
            },
            containerColor = JtSurface
        )
    }

    // Modal Renombrar libro
    documentToRename?.let { doc ->
        var newTitle by remember { mutableStateOf(doc.title) }

        AlertDialog(
            onDismissRequest = { documentToRename = null },
            title = {
                Text("Cambiar nombre del libro", fontWeight = FontWeight.Bold, color = JtPrimaryText)
            },
            text = {
                Column {
                    Text("Ingresa el nuevo título para este documento:", color = JtSecondaryText, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.cambiarNombreDocumento(doc.id, newTitle)
                            documentToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                    enabled = newTitle.isNotBlank()
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToRename = null }) {
                    Text("Cancelar", color = JtSecondaryText)
                }
            },
            containerColor = JtSurface
        )
    }

    // Modal Cambiar Carátula
    documentToChangeCover?.let { doc ->
        val presetColors = listOf(
            0xFF1C3829 to "Verde JurisTech",
            0xFF1A2A3A to "Azul Marino Judicial",
            0xFF3E1F1F to "Burdeos Borgoña",
            0xFF2D2B1F to "Bronce Antiguo",
            0xFF181818 to "Negro Cuero"
        )

        AlertDialog(
            onDismissRequest = { documentToChangeCover = null },
            title = {
                Text("Personalizar Carátula", fontWeight = FontWeight.Bold, color = JtPrimaryText)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Puedes subir una foto desde tu galería o elegir un color de piel clásica:",
                        color = JtSecondaryText,
                        fontSize = 14.sp
                    )

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Elegir foto de mi galería", color = Color.White)
                    }

                    HorizontalDivider(color = JtBorder)

                    Text("O elige un color clásico:", fontWeight = FontWeight.Medium, color = JtPrimaryText, fontSize = 14.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        presetColors.forEach { (colorVal, _) ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .border(2.dp, JtGold, CircleShape)
                                    .clickable {
                                        viewModel.cambiarCaratulaDocumento(doc.id, null, colorVal)
                                        documentToChangeCover = null
                                    }
                            )
                        }
                    }

                    if (doc.customCoverUri != null) {
                        TextButton(
                            onClick = {
                                viewModel.cambiarCaratulaDocumento(doc.id, null, null)
                                documentToChangeCover = null
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Restablecer carátula predeterminada", color = JtSecondaryText, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { documentToChangeCover = null }) {
                    Text("Cerrar", color = JtSecondaryText)
                }
            },
            containerColor = JtSurface
        )
    }

    // Modal Editar Metadatos y Clasificación
    documentToEditMetadata?.let { doc ->
        var editTitle by remember { mutableStateOf(doc.title) }
        var editAuthor by remember { mutableStateOf(doc.author) }
        var editMateria by remember { mutableStateOf(doc.materia) }
        var editYear by remember { mutableStateOf(doc.year) }
        var editDesc by remember { mutableStateOf(doc.description) }
        var editCollection by remember { mutableStateOf(doc.collection) }
        var newTagInput by remember { mutableStateOf("") }
        val currentTags = remember {
            val initialList = try {
                val arr = org.json.JSONArray(doc.tagsJson)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                list
            } catch (_: Exception) { mutableListOf<String>() }
            mutableStateListOf(*initialList.toTypedArray())
        }

        AlertDialog(
            onDismissRequest = { documentToEditMetadata = null },
            title = {
                Text("Detalles del Documento", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = JtPrimaryText)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Título") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAuthor,
                        onValueChange = { editAuthor = it },
                        label = { Text("Autor / Entidad emisora") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editMateria,
                            onValueChange = { editMateria = it },
                            label = { Text("Materia (Penal, Civil...)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editYear,
                            onValueChange = { editYear = it },
                            label = { Text("Año") },
                            singleLine = true,
                            modifier = Modifier.width(90.dp)
                        )
                    }
                    OutlinedTextField(
                        value = editCollection,
                        onValueChange = { editCollection = it },
                        label = { Text("Colección / Carpeta") },
                        placeholder = { Text("Ej: Códigos Fundamentales") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Descripción o notas") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Etiquetas:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = JtPrimaryText)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTagInput,
                            onValueChange = { newTagInput = it },
                            placeholder = { Text("Nueva etiqueta") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (newTagInput.isNotBlank() && !currentTags.contains(newTagInput.trim())) {
                                    currentTags.add(newTagInput.trim())
                                    newTagInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(JtGreenPrimary, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir etiqueta", tint = Color.White)
                        }
                    }

                    if (currentTags.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentTags.forEach { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = { currentTags.remove(tag) },
                                    label = { Text(tag, fontSize = 12.sp) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.actualizarMetadatosDocumento(
                            id = doc.id,
                            title = editTitle,
                            author = editAuthor,
                            materia = editMateria,
                            year = editYear,
                            description = editDesc,
                            collection = editCollection,
                            tags = currentTags.toList()
                        )
                        documentToEditMetadata = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                    enabled = editTitle.isNotBlank()
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToEditMetadata = null }) {
                    Text("Cancelar", color = JtSecondaryText)
                }
            },
            containerColor = JtSurface
        )
    }

    // Modal Crear Nueva Colección
    if (showNewCollectionDialog) {
        var collectionName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewCollectionDialog = false },
            title = { Text("Nueva Colección / Carpeta", fontWeight = FontWeight.Bold, color = JtPrimaryText) },
            text = {
                Column {
                    Text("Crea una colección para clasificar tus textos jurídicos:", color = JtSecondaryText, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = collectionName,
                        onValueChange = { collectionName = it },
                        placeholder = { Text("Ej: Derecho Penal Hondureño") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (collectionName.isNotBlank()) {
                            viewModel.setCollectionFilter(collectionName.trim())
                            showNewCollectionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                    enabled = collectionName.isNotBlank()
                ) {
                    Text("Crear y Filtrar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCollectionDialog = false }) {
                    Text("Cancelar", color = JtSecondaryText)
                }
            },
            containerColor = JtSurface
        )
    }

    if (uiState.showStudyCoachDialog) {
        com.example.ui.learning.StudyCoachDialog(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.setShowStudyCoachDialog(false) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryHomeContent(
    documents: List<RecentDocumentEntity>,
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onSelectDocument: (RecentDocumentEntity) -> Unit,
    onAddDocument: () -> Unit,
    onRenameDocument: (RecentDocumentEntity) -> Unit,
    onChangeCover: (RecentDocumentEntity) -> Unit,
    onEditMetadata: (RecentDocumentEntity) -> Unit,
    onShareDocument: (RecentDocumentEntity) -> Unit,
    onDeleteDocument: (RecentDocumentEntity) -> Unit,
    onAddNewCollection: () -> Unit
) {
    if (documents.isEmpty()) {
        EmptyLibraryView(onAddDocument = onAddDocument)
    } else {
        FilledLibraryView(
            documents = documents,
            uiState = uiState,
            viewModel = viewModel,
            onSelectDocument = onSelectDocument,
            onAddDocument = onAddDocument,
            onRenameDocument = onRenameDocument,
            onChangeCover = onChangeCover,
            onEditMetadata = onEditMetadata,
            onShareDocument = onShareDocument,
            onDeleteDocument = onDeleteDocument,
            onAddNewCollection = onAddNewCollection
        )
    }
}

@Composable
private fun EmptyLibraryView(onAddDocument: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = JtSecondaryText,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tu biblioteca está vacía",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = JtPrimaryText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Añade códigos de leyes, decretos o contratos para comenzar a escuchar y anotar.",
            fontSize = 14.sp,
            color = JtSecondaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddDocument,
            colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Añadir Libro o Código", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun FilledLibraryView(
    documents: List<RecentDocumentEntity>,
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel,
    onSelectDocument: (RecentDocumentEntity) -> Unit,
    onAddDocument: () -> Unit,
    onRenameDocument: (RecentDocumentEntity) -> Unit,
    onChangeCover: (RecentDocumentEntity) -> Unit,
    onEditMetadata: (RecentDocumentEntity) -> Unit,
    onShareDocument: (RecentDocumentEntity) -> Unit,
    onDeleteDocument: (RecentDocumentEntity) -> Unit,
    onAddNewCollection: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Filtrado y ordenamiento de documentos
    val availableCollections = remember(documents) {
        listOf("Todas") + documents.mapNotNull { it.collection.takeIf { c -> c.isNotBlank() } }.distinct()
    }

    val displayDocs = remember(documents, uiState.selectedCollection, uiState.selectedTagFilter, uiState.librarySortOrder) {
        documents.filter { doc ->
            val matchCol = uiState.selectedCollection.isEmpty() || uiState.selectedCollection == "Todas" || doc.collection.equals(uiState.selectedCollection, ignoreCase = true)
            val matchTag = uiState.selectedTagFilter == null || doc.tagsJson.contains(uiState.selectedTagFilter)
            matchCol && matchTag
        }.let { list ->
            when (uiState.librarySortOrder) {
                com.example.model.LibrarySortOrder.RECENT -> list.sortedByDescending { it.lastAccessedTimestamp }
                com.example.model.LibrarySortOrder.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
                com.example.model.LibrarySortOrder.TITLE_DESC -> list.sortedByDescending { it.title.lowercase() }
                com.example.model.LibrarySortOrder.AUTHOR -> list.sortedBy { it.author.lowercase() }
                com.example.model.LibrarySortOrder.PROGRESS -> list.sortedByDescending { if (it.totalPages > 0) it.lastPage.toFloat() / it.totalPages else 0f }
            }
        }
    }

    val actualDocs = if (displayDocs.isNotEmpty()) displayDocs else documents
    val pagerState = rememberPagerState(pageCount = { actualDocs.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 76.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Barra superior con título y botón de Añadir libro
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("JurisTech Legal Reader", color = JtSecondaryText, fontSize = 12.sp, letterSpacing = 1.sp)
                Text(
                    text = "JurisTech",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = JtPrimaryText
                )
            }

            Button(
                onClick = onAddDocument,
                colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("top_add_book_button")
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Añadir libro",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Banner de notificación de biblioteca (e.g. Libro añadido con éxito)
        uiState.libraryFeedbackMessage?.let { msg ->
            Surface(
                color = JtGreenPrimary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JtGreenPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = JtGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = JtPrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearLibraryFeedbackMessage() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = JtSecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Banner de Repaso Espaciado si hay conceptos pendientes hoy
        if (uiState.dueCardsCount > 0) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { viewModel.setShowStudyCoachDialog(true, 1) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Repaso",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Repaso Espaciado Pendiente",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${uiState.dueCardsCount} conceptos para consolidar hoy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ir a Repaso",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Barra de Colecciones y Filtros
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chips de Colecciones horizontales
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                availableCollections.forEach { col ->
                    val isSelected = (uiState.selectedCollection.isEmpty() && col == "Todas") || uiState.selectedCollection == col
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCollectionFilter(if (col == "Todas") "" else col) },
                        label = { Text(col, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JtGreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Botón para crear nueva colección
                AssistChip(
                    onClick = onAddNewCollection,
                    label = { Text("+ Colección", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )

                // Botón rápido para añadir libro
                AssistChip(
                    onClick = onAddDocument,
                    label = { Text("+ Añadir Libro", fontSize = 12.sp, color = JtGreenPrimary, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = JtGreenPrimary, modifier = Modifier.size(14.dp)) }
                )
            }

            // Menú de ordenamiento
            var showSortMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showSortMenu = true }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Sort, contentDescription = "Ordenar biblioteca", tint = JtGreenPrimary)
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(JtSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Más recientes", color = JtPrimaryText) },
                        onClick = {
                            viewModel.setLibrarySortOrder(com.example.model.LibrarySortOrder.RECENT)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nombre (A - Z)", color = JtPrimaryText) },
                        onClick = {
                            viewModel.setLibrarySortOrder(com.example.model.LibrarySortOrder.TITLE_ASC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nombre (Z - A)", color = JtPrimaryText) },
                        onClick = {
                            viewModel.setLibrarySortOrder(com.example.model.LibrarySortOrder.TITLE_DESC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Por Autor", color = JtPrimaryText) },
                        onClick = {
                            viewModel.setLibrarySortOrder(com.example.model.LibrarySortOrder.AUTHOR)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Por Progreso de lectura", color = JtPrimaryText) },
                        onClick = {
                            viewModel.setLibrarySortOrder(com.example.model.LibrarySortOrder.PROGRESS)
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pager de carátulas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 96.dp),
                pageSpacing = 14.dp,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val doc = actualDocs[page]
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                val scale = 1f - (pageOffset * 0.15f).coerceIn(0f, 0.25f)
                val isCenter = page == pagerState.currentPage

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .fillMaxHeight()
                        .width(150.dp)
                        .clickable {
                            if (isCenter) {
                                onSelectDocument(doc)
                            } else {
                                coroutineScope.launch { pagerState.animateScrollToPage(page) }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BookCover(
                        title = doc.title,
                        isCenter = isCenter,
                        customCoverUri = doc.customCoverUri,
                        coverColor = doc.coverColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Controles de navegación del carrusel
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
                    .size(32.dp)
                    .background(JtGreenPrimary, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Anterior", tint = Color.White)
            }

            Text(
                text = "${pagerState.currentPage + 1} / ${actualDocs.size}",
                color = JtSecondaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        if (pagerState.currentPage < actualDocs.size - 1) pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .size(32.dp)
                    .background(JtGreenPrimary, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Siguiente", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Info del documento seleccionado
        val currentDoc = actualDocs.getOrNull(pagerState.currentPage) ?: actualDocs.first()

        Text(
            text = currentDoc.title,
            color = JtPrimaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        val docSubtitle = buildString {
            if (currentDoc.author.isNotBlank()) append(currentDoc.author)
            if (currentDoc.materia.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(currentDoc.materia)
            }
            if (currentDoc.year.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append("(${currentDoc.year})")
            }
        }

        if (docSubtitle.isNotBlank()) {
            Text(
                text = docSubtitle,
                color = JtSecondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Botones rápidos para acciones del documento (Renombrar, Carátula, Detalles, Compartir, Eliminar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AssistChip(
                onClick = { onRenameDocument(currentDoc) },
                label = { Text("Renombrar", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp)) }
            )
            AssistChip(
                onClick = { onChangeCover(currentDoc) },
                label = { Text("Carátula", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(13.dp)) }
            )
            AssistChip(
                onClick = { onEditMetadata(currentDoc) },
                label = { Text("Detalles", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(13.dp), tint = JtGreenPrimary) }
            )
            AssistChip(
                onClick = { onShareDocument(currentDoc) },
                label = { Text("Compartir", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp)) }
            )
            AssistChip(
                onClick = { onDeleteDocument(currentDoc) },
                label = { Text("Eliminar", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.error) }
            )
        }

        // Barra de progreso
        val totalPages = if (currentDoc.totalPages > 0) currentDoc.totalPages else 1
        val percent = ((currentDoc.lastPage.toFloat() / totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)

        Row(
            modifier = Modifier.fillMaxWidth(0.82f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Última lectura: Pág. ${currentDoc.lastPage} de $totalPages", color = JtSecondaryText, fontSize = 12.sp)
            Text("$percent%", color = JtGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = JtGreenPrimary,
            trackColor = JtBorder
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botones de acción principales: Leer ahora y Añadir libro
        Row(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onSelectDocument(currentDoc) },
                colors = ButtonDefaults.buttonColors(containerColor = JtGreenPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
                    .testTag("continue_reading_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Leer ahora", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = onAddDocument,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = JtGreenPrimary.copy(alpha = 0.15f),
                    contentColor = JtGreenPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("shelf_add_book_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = JtGreenPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir libro", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun BookCover(
    title: String,
    isCenter: Boolean,
    customCoverUri: String? = null,
    coverColor: Long? = null
) {
    val defaultBaseColor = if (coverColor != null) Color(coverColor) else if (isCenter) JtGreenPrimary else Color(0xFF7A7A73)
    val spineColor = if (coverColor != null) Color(coverColor).copy(alpha = 0.7f) else if (isCenter) JtGreenSecondary else Color(0xFF63635D)
    val textColor = if (isCenter) JtGold else Color(0xFFE8E8E8)
    val hasCustomCover = !customCoverUri.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = if (isCenter) 16.dp else 4.dp,
                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, topStart = 4.dp, bottomStart = 4.dp)
            )
            .background(defaultBaseColor, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, topStart = 4.dp, bottomStart = 4.dp))
    ) {
        if (hasCustomCover) {
            // Foto de carátula nítida y limpia por encima de todo, sin textos ni sombras oscuras superpuestas
            AsyncImage(
                model = customCoverUri,
                contentDescription = "Carátula de $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, topStart = 4.dp, bottomStart = 4.dp))
            )
        } else {
            // Diseño de libro encuadernado predeterminado solo si no hay foto personalizada
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
                    .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title.uppercase(),
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )

                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(44.dp)
                )

                Text(
                    text = "JurisTech",
                    color = textColor.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LibrarySearchContent(
    uiState: JurisTechUiState,
    onSearch: (String) -> Unit,
    onSelectResult: (SearchResultItem) -> Unit
) {
    var queryText by remember { mutableStateOf(uiState.searchQuery) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Búsqueda Jurídica",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = JtPrimaryText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Busca artículos, palabras clave o títulos en toda tu biblioteca",
            color = JtSecondaryText,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = queryText,
            onValueChange = {
                queryText = it
                onSearch(it)
            },
            placeholder = { Text("Ej. Habeas Corpus, Prescripción, Infracción...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = JtGreenPrimary) },
            trailingIcon = {
                if (queryText.isNotBlank()) {
                    IconButton(onClick = {
                        queryText = ""
                        onSearch("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isSearching) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = JtGreenPrimary)
            }
        } else if (queryText.isNotBlank() && uiState.searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No se encontraron coincidencias para \"$queryText\"",
                    color = JtSecondaryText,
                    textAlign = TextAlign.Center
                )
            }
        } else if (queryText.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.FindInPage, contentDescription = null, tint = JtSecondaryText, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Escribe para buscar en tus documentos",
                    color = JtSecondaryText,
                    fontSize = 14.sp
                )
            }
        } else {
            Text(
                "${uiState.searchResults.size} resultados encontrados",
                fontWeight = FontWeight.Medium,
                color = JtPrimaryText,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.searchResults) { result ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectResult(result) },
                        colors = CardDefaults.cardColors(containerColor = JtSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    result.documentTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = JtPrimaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = JtBorder,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "Pág. ${result.pageNumber}",
                                        fontSize = 11.sp,
                                        color = JtSecondaryText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                result.matchedSnippet,
                                fontSize = 13.sp,
                                color = JtSecondaryText,
                                lineHeight = 18.sp,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySettingsContent(
    uiState: JurisTechUiState,
    viewModel: JurisTechViewModel
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "Ajustes y Configuración",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = JtPrimaryText
        )

        // Sección Velocidad de Lectura
        Card(
            colors = CardDefaults.cardColors(containerColor = JtSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = JtGreenPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Velocidad de Voz Predeterminada", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = JtPrimaryText)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Ajusta la velocidad del lector sintético para mayor agilidad:",
                    color = JtSecondaryText,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                val rates = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rates.forEach { rate ->
                        val selected = uiState.speechRate == rate
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setSpeechRate(rate) },
                            label = { Text("${rate}x", fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JtGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Sección Tamaño de Letra
        Card(
            colors = CardDefaults.cardColors(containerColor = JtSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatSize, contentDescription = null, tint = JtGreenPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Tamaño del Texto de Lectura", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = JtPrimaryText)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Elige la escala de fuente para leer con comodidad:",
                    color = JtSecondaryText,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                val sizes = listOf(0.85f to "Pequeño", 1.0f to "Normal", 1.15f to "Grande", 1.35f to "Muy grande")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    sizes.forEach { (scale, label) ->
                        val selected = uiState.fontSizeScale == scale
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setFontSizeScale(scale) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JtGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Sección Espaciado de Líneas
        Card(
            colors = CardDefaults.cardColors(containerColor = JtSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatLineSpacing, contentDescription = null, tint = JtGreenPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Espaciado entre Líneas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = JtPrimaryText)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Separa las líneas del texto para una lectura jurídica más relajada:",
                    color = JtSecondaryText,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                val spacings = listOf(1.2f to "Compacto", 1.5f to "Estándar", 1.8f to "Espacioso", 2.2f to "Amplio")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    spacings.forEach { (spVal, label) ->
                        val selected = kotlin.math.abs(uiState.lineSpacing - spVal) < 0.05f
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setLineSpacing(spVal) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JtGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Sección Preferencias de Resaltado y Subrayado
        Card(
            colors = CardDefaults.cardColors(containerColor = JtSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BorderColor, contentDescription = null, tint = JtGreenPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Estilo de Resaltado Jurídico", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = JtPrimaryText)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Selecciona el color predeterminado para tus apuntes:", color = JtSecondaryText, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))

                val highlightColors = listOf(
                    0xFFFFF9C4 to "Amarillo",
                    0xFFC8E6C9 to "Verde",
                    0xFFBBDEFB to "Azul",
                    0xFFF8BBD0 to "Rosa",
                    0xFFFFE0B2 to "Naranja"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    highlightColors.forEach { (hexVal, name) ->
                        val hexStr = String.format("#%06X", 0xFFFFFF and hexVal.toInt())
                        val isSelected = uiState.activeHighlightColorHex.equals(hexStr, ignoreCase = true)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { viewModel.setActiveHighlightColor(hexStr) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(hexVal))
                                    .border(if (isSelected) 3.dp else 1.dp, if (isSelected) JtGreenPrimary else JtBorder, CircleShape)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(name, fontSize = 10.sp, color = JtSecondaryText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo Subrayado", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = JtPrimaryText)
                        Text("Subraya las líneas en lugar de pintar el fondo", fontSize = 12.sp, color = JtSecondaryText)
                    }
                    Switch(
                        checked = uiState.isUnderlineMode,
                        onCheckedChange = { viewModel.setIsUnderlineMode(it) }
                    )
                }
            }
        }

        // Sección Motor de Voz
        Card(
            colors = CardDefaults.cardColors(containerColor = JtSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = JtGreenPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Motor de Voz (TTS)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = JtPrimaryText)
                        Text("Solo dos opciones: Voz de Hombre y Voz de Mujer", fontSize = 12.sp, color = JtSecondaryText)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Estado de Inteligencia Artificial / Modo de Conexión
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (uiState.isNetworkAvailable) JtGreenPrimary.copy(alpha = 0.12f) else JtGold.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.isNetworkAvailable) Icons.Default.AutoAwesome else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (uiState.isNetworkAvailable) JtGreenPrimary else JtGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isNetworkAvailable) {
                                "En línea: Voces con Inteligencia Artificial activas (entonación y prosodia humana natural)."
                            } else {
                                "Sin internet: Modo Offline activo (voces locales del dispositivo sin conexión garantizadas)."
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.isNetworkAvailable) JtGreenPrimary else JtGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Las dos opciones exclusivas: Voz de Hombre y Voz de Mujer
                listOf(
                    com.example.tts.VoiceGender.HOMBRE,
                    com.example.tts.VoiceGender.MUJER
                ).forEach { gender ->
                    val isSelected = uiState.selectedVoiceGender == gender
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) JtGreenPrimary.copy(alpha = 0.1f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) JtGreenPrimary else JtBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.setVoiceGender(gender) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setVoiceGender(gender) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = JtGreenPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = gender.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = JtPrimaryText
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (uiState.isNetworkAvailable) JtGreenPrimary else JtSecondaryText
                                        ) {
                                            Text(
                                                text = if (uiState.isNetworkAvailable) "IA Natural" else "Offline",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (gender == com.example.tts.VoiceGender.HOMBRE) {
                                            "Entonación masculina natural, timbre barítono claro y pausado"
                                        } else {
                                            "Entonación femenina natural, timbre cálido, expresivo y fluido"
                                        },
                                        fontSize = 12.sp,
                                        color = JtSecondaryText
                                    )
                                }
                            }

                            // Botón de muestra de audio interactivo
                            OutlinedButton(
                                onClick = { viewModel.previewVoice(gender) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isSelected) JtGreenPrimary else JtPrimaryText
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Escuchar muestra",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Escuchar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Idioma: Español (es-ES / es-HN)", color = JtSecondaryText, fontSize = 12.sp)
                Text("Ambas opciones funcionan con IA cuando hay internet y en offline cuando no hay internet.", color = JtSecondaryText, fontSize = 11.sp)
            }
        }

        // Sección Gestión de Datos
        Card(
            colors = CardDefaults.cardColors(containerColor = JtSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = JtGreenPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Gestión de Biblioteca", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = JtPrimaryText)
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.restaurarCodigosHonduras() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restaurar Códigos de Honduras")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { viewModel.limpiarTodaLaInformacion() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vaciar Biblioteca")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
