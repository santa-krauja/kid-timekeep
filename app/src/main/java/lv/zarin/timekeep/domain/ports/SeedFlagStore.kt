package lv.zarin.timekeep.domain.ports

interface SeedFlagStore {
    suspend fun isSeeded(): Boolean
    suspend fun markSeeded()
}
