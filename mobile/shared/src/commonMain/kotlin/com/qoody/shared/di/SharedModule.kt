package com.qoody.shared.di

import com.qoody.shared.home.DefaultHomeRepository
import com.qoody.shared.home.HomeRepository
import com.qoody.shared.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val sharedModule =
    module {
        single { DefaultHomeRepository() } bind HomeRepository::class
        viewModelOf(::HomeViewModel)
    }
