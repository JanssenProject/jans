package com.example.fido2.ui.screens.splash

import androidx.lifecycle.ViewModel

class SplashScreenViewModel(

) : ViewModel() {

//    private val job = SupervisorJob()
//    private val coroutineContext: CoroutineContext = job + Dispatchers.IO
//    private val viewModelScope = CoroutineScope(coroutineContext)
//
//    val newsViewState = mutableStateOf<HomeScreenViewState>(HomeScreenViewState.Loading)
//
//    init {
//        getPhotos()
//    }
//
//    private fun getPhotos() {
//        viewModelScope.launch {
//            allPhotosUseCase.invoke(false).onSuccess {
//                newsViewState.value = HomeScreenViewState.Success(photos = it)
//            }.onFailure {
//                newsViewState.value = HomeScreenViewState.Failure(it.message.toString())
//            }
//        }
//    }
}