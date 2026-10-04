package com.qoody.shared.home

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun emitsGreetingFromRepository() =
        runTest {
            val viewModel =
                HomeViewModel(
                    object : HomeRepository {
                        override suspend fun greeting() = "Hello"
                    },
                )

            viewModel.uiState.test {
                assertEquals(HomeUiState.Success("Hello"), awaitItem())
            }
        }
}
