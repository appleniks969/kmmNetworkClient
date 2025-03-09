package com.kmp.network.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kmm.networkclient.NetworkClient
import com.kmm.networkclient.NetworkClientConfig
import com.kmm.networkclient.NetworkException
import com.kmm.networkclient.samples.User
import kotlinx.coroutines.launch

@Composable
fun DemoScreen() {
    // Create a NetworkClient instance with proper configuration
    val networkClient = remember { 
        NetworkClient(
            NetworkClientConfig(
                baseUrl = "https://jsonplaceholder.typicode.com",
                enableLogging = true,
                retryConfig = NetworkClientConfig.RetryConfig(
                    maxRetries = 2
                )
            )
        ) 
    }
    
    // Remember to close the client when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            networkClient.close()
        }
    }
    
    val scope = rememberCoroutineScope()
    
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Load users when the screen is first displayed
    LaunchedEffect(Unit) {
        loading = true
        fetchUsers(networkClient) { result ->
            loading = false
            result.fold(
                onSuccess = { userList -> 
                    users = userList
                    error = null
                },
                onFailure = { e ->
                    error = e.message ?: "Unknown error"
                }
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KMM Network Client Demo") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            error = null
                            loading = true
                            scope.launch {
                                fetchUsers(networkClient) { result ->
                                    loading = false
                                    result.fold(
                                        onSuccess = { userList -> 
                                            users = userList
                                            error = null
                                        },
                                        onFailure = { e ->
                                            error = e.message ?: "Unknown error"
                                        }
                                    )
                                }
                            }
                        }
                    ) {
                        Text("Retry")
                    }
                }
            } else {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Users (${users.size})",
                            style = MaterialTheme.typography.h6
                        )
                        Button(
                            onClick = {
                                loading = true
                                scope.launch {
                                    fetchUsers(networkClient) { result ->
                                        loading = false
                                        result.fold(
                                            onSuccess = { userList -> 
                                                users = userList
                                                error = null
                                            },
                                            onFailure = { e ->
                                                error = e.message ?: "Unknown error"
                                            }
                                        )
                                    }
                                }
                            }
                        ) {
                            Text("Refresh")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (users.isEmpty()) {
                        Text(
                            text = "No users found",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.body1
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(users) { user ->
                                UserItem(user)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserItem(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.h6
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.email,
                style = MaterialTheme.typography.body2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ID: ${user.id}",
                style = MaterialTheme.typography.caption
            )
        }
    }
}

private suspend fun fetchUsers(
    networkClient: NetworkClient,
    onResult: (Result<List<User>>) -> Unit
) {
    try {
        // Make a real API call to JSONPlaceholder
        val users: List<User> = networkClient.get("/users")
        onResult(Result.success(users))
    } catch (e: NetworkException) {
        onResult(Result.failure(e))
    } catch (e: Exception) {
        onResult(Result.failure(e))
    }
} 