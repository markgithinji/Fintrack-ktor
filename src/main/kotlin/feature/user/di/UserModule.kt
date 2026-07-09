package feature.user.di

import com.fintrack.feature.user.data.UserRepositoryImpl
import feature.user.domain.UserRepository
import feature.user.domain.UserService
import feature.user.domain.UserServiceImpl
import com.fintrack.feature.accounts.domain.repository.AccountsRepository
import com.fintrack.core.EmailService
import feature.auth.domain.repository.EmailVerificationRepository
import org.koin.dsl.module

fun userModule() = module {
    single<UserRepository> { UserRepositoryImpl() }
    single<UserService> { 
        UserServiceImpl(
            get<UserRepository>(), 
            get<AccountsRepository>(), 
            get<EmailService>(), 
            get<EmailVerificationRepository>()
        ) 
    }
}
