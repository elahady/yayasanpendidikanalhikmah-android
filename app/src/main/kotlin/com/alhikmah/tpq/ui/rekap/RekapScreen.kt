package com.alhikmah.tpq.ui.rekap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.alhikmah.tpq.LocalAppContainer
import com.alhikmah.tpq.domain.api.TpqRepository
import com.alhikmah.tpq.domain.fake.FakeData
import com.alhikmah.tpq.domain.model.Rekap
import com.alhikmah.tpq.domain.model.RekapCounts
import com.alhikmah.tpq.domain.util.Load
import com.alhikmah.tpq.domain.util.Tanggal
import com.alhikmah.tpq.domain.util.attempt
import com.alhikmah.tpq.domain.util.toLoad
import com.alhikmah.tpq.ui.common.DateBar
import com.alhikmah.tpq.ui.common.LoadContent
import com.alhikmah.tpq.ui.common.TpqTopBar
import com.alhikmah.tpq.ui.theme.AlHikmahTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RekapUiState(
    val tanggal: LocalDate,
    val load: Load<Rekap> = Load.Loading,
)

class RekapViewModel(private val repo: TpqRepository) : ViewModel() {
    private val _state = MutableStateFlow(RekapUiState(Tanggal.today()))
    val state: StateFlow<RekapUiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        load()
    }

    fun setTanggal(date: LocalDate) {
        _state.value = RekapUiState(date)
        load()
    }

    fun load() {
        job?.cancel()
        val date = _state.value.tanggal
        _state.value = RekapUiState(date, Load.Loading)
        job = viewModelScope.launch {
            val load = attempt { repo.rekap(Tanggal.toApi(date)) }.toLoad()
            _state.value = RekapUiState(date, load)
        }
    }
}

@Composable
fun RekapScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: RekapViewModel = viewModel(factory = viewModelFactory { initializer { RekapViewModel(container.repository) } })
    val state by vm.state.collectAsStateWithLifecycle()
    RekapContent(state, onBack, vm::setTanggal, vm::load)
}

@Composable
fun RekapContent(state: RekapUiState, onBack: () -> Unit, onTanggal: (LocalDate) -> Unit, onRetry: () -> Unit) {
    Scaffold(topBar = { TpqTopBar("Rekap Harian", onBack) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DateBar(state.tanggal, onTanggal, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            LoadContent(state.load, onRetry) { rekap ->
                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RekapCard("Santri", rekap.santri)
                    RekapCard("Ustadz / Ustadzah", rekap.ustadz)
                }
            }
        }
    }
}

@Composable
private fun RekapCard(title: String, counts: RekapCounts) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                Text("${counts.hadir}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Text(" dari ${counts.total} hadir", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 6.dp))
            }
            LinearProgressIndicator(
                progress = { if (counts.total > 0) counts.hadir.toFloat() / counts.total else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            val tidakHadir = (counts.total - counts.hadir - counts.belumDicatat).coerceAtLeast(0)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stat("Tidak hadir", tidakHadir)
                Stat("Belum dicatat", counts.belumDicatat, highlight = counts.belumDicatat > 0)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, highlight: Boolean = false) {
    Column {
        Text("$value", style = MaterialTheme.typography.titleLarge, color = if (highlight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true, name = "Rekap")
@Composable
private fun RekapPreview() {
    AlHikmahTheme(darkTheme = false) {
        RekapContent(RekapUiState(LocalDate.of(2026, 11, 2), Load.Success(FakeData.rekap("2026-11-02"))), {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Rekap gelap")
@Composable
private fun RekapDarkPreview() {
    AlHikmahTheme(darkTheme = true) {
        RekapContent(RekapUiState(LocalDate.of(2026, 11, 2), Load.Success(FakeData.rekap("2026-11-02"))), {}, {}, {})
    }
}
