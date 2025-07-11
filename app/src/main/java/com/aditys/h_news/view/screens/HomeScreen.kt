package com.aditys.h_news.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aditys.h_news.model.SearchResult
import com.aditys.h_news.model.Job
import com.aditys.h_news.viewmodel.HomeViewModel
import com.aditys.h_news.viewmodel.NewsFilter
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.aditys.h_news.model.ItemResponse
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.AnnotatedString
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

fun epochToDateString(epoch: Int): String {
    val date = Date(epoch * 1000L)
    val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return format.format(date)
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onPostClick: (SearchResult) -> Unit = {},
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Find the most upvoted post of the current month
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    val trending2024 = uiState.newsList
        .filter { item ->
            item.created_at_i?.let {
                val cal = Calendar.getInstance().apply { timeInMillis = it * 1000L }
                cal.get(Calendar.YEAR) == 2024
            } ?: false
        }
        .maxByOrNull { it.points ?: 0 }

    val trendingPost = trending2024
        ?: uiState.newsList.maxByOrNull { it.points ?: 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (uiState.selectedFilter == NewsFilter.TRENDING) {
            Text("Most Trending", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            trendingPost?.let { post ->
                Box(modifier = Modifier.clickable {
                    val id = post.objectID?.toIntOrNull() ?: return@clickable
                    coroutineScope.launch {
                        val fullItem = viewModel.fetchFullNewsItem(id)
                        navController.navigate(
                            "newsDetail/" +
                            "${android.net.Uri.encode(fullItem?.title ?: post.title ?: "")}/" +
                            "${android.net.Uri.encode(fullItem?.author ?: post.author ?: "")}/" +
                            "${android.net.Uri.encode(fullItem?.text ?: "No content available")}/" +
                            "${android.net.Uri.encode(fullItem?.url ?: post.url ?: "")}"
                        )
                    }
                }) {
                    TrendingCard(post)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = { viewModel.onFilterSelected(NewsFilter.TRENDING) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4A261))
        ) {
            Text("Explore top trending", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text("See what's happening", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FilterButtons(
            selected = uiState.selectedFilter,
            onSelected = { viewModel.onFilterSelected(it) }
        )
        Spacer(Modifier.height(16.dp))
        if (uiState.isLoading) {
            CircularProgressIndicator(color = Color.White)
        } else if (uiState.error != null) {
            Text("Error: ${uiState.error}", color = Color.Red)
        } else {
            when (uiState.selectedFilter) {
                NewsFilter.JOBS -> JobsList(uiState.jobsList)
                NewsFilter.PAST -> {
                    val sortedNews = uiState.newsList.sortedByDescending { item ->
                        item.created_at_i ?: 0
                    }
                    NewsList(sortedNews, onPostClick = { item ->
                        val id = item.objectID?.toIntOrNull() ?: return@NewsList
                        coroutineScope.launch {
                            val fullItem = viewModel.fetchFullNewsItem(id)
                            navController.navigate(
                                "newsDetail/" +
                                "${android.net.Uri.encode(fullItem?.title ?: item.title ?: "")}/" +
                                "${android.net.Uri.encode(fullItem?.author ?: item.author ?: "")}/" +
                                "${android.net.Uri.encode(fullItem?.text ?: "No content available")}/" +
                                "${android.net.Uri.encode(fullItem?.url ?: item.url ?: "")}"
                            )
                        }
                    })
                }
                else -> NewsList(uiState.newsList, onPostClick = { item ->
                    val id = item.objectID?.toIntOrNull() ?: return@NewsList
                    coroutineScope.launch {
                        val fullItem = viewModel.fetchFullNewsItem(id)
                        navController.navigate(
                            "newsDetail/" +
                            "${android.net.Uri.encode(fullItem?.title ?: item.title ?: "")}/" +
                            "${android.net.Uri.encode(fullItem?.author ?: item.author ?: "")}/" +
                            "${android.net.Uri.encode(fullItem?.text ?: "No content available")}/" +
                            "${android.net.Uri.encode(fullItem?.url ?: item.url ?: "")}"
                        )
                    }
                })
            }
        }
    }
}

@Composable
fun TrendingCard(item: SearchResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2832), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = item.title ?: "",
            color = Color(0xFFF4A261),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.story_text ?: item.url ?: "",
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Text("${item.points ?: 0} points", color = Color(0xFFF4A261))
            Spacer(Modifier.width(16.dp))
            Text("${item.num_comments ?: 0} comments", color = Color(0xFFF4A261))
        }
    }
}

@Composable
fun FilterButtons(selected: NewsFilter, onSelected: (NewsFilter) -> Unit) {
    val filters = listOf(
        NewsFilter.NEW to "new",
        NewsFilter.PAST to "past",
        NewsFilter.SHOW to "show",
        NewsFilter.JOBS to "jobs"
    )
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth()
    ) {
        filters.forEach { (filter, label) ->
            val isSelected = selected == filter
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .background(
                        if (isSelected) Color(0xFFF4A261) else Color(0xFF2D2832),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelected(filter) }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    label,
                    color = if (isSelected) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NewsList(news: List<SearchResult>, onPostClick: (SearchResult) -> Unit) {
    Column {
        news.forEach { item ->
            NewsCard(item, onClick = { onPostClick(item) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun NewsCard(item: SearchResult, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2832), RoundedCornerShape(8.dp))
            .padding(12.dp)
            .clickable { onClick() }
    ) {
        // Show title or story_title or fallback
        val heading = item.title?.takeIf { it.isNotBlank() }
            ?: item.story_title?.takeIf { it.isNotBlank() }
            ?: "No Title"
        Text(heading, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("by ${item.author ?: "unknown"}", color = Color(0xFFF4A261))
        Spacer(Modifier.height(4.dp))
        // Show story_text only if available (do not show fallback)
        val details = item.story_text?.takeIf { it.isNotBlank() }
        if (!details.isNullOrBlank()) {
            Text(details, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
        }
        // Date/time
        item.created_at?.let {
            Text("Posted: $it", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        item.created_at_i?.let {
            Text("Posted: ${epochToDateString(it)}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(4.dp))
        Row {
            Text("${item.points ?: 0} points", color = Color(0xFFF4A261))
            Spacer(Modifier.width(16.dp))
            Text("${item.num_comments ?: 0} comments", color = Color(0xFFF4A261))
        }
    }
}

@Composable
fun JobsList(jobs: List<Job>) {
    val context = LocalContext.current
    Column {
        jobs.forEach { job ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2832), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(job.title ?: "", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(job.author ?: "", color = Color(0xFFF4A261))
                Spacer(Modifier.height(4.dp))
                job.url?.let {
                    ClickableText(
                        text = AnnotatedString(it),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFF4A261)),
                        onClick = { _ ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                            context.startActivity(intent)
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}