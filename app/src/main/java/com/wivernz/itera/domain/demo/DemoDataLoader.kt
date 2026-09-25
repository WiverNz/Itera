package com.wivernz.itera.domain.demo

/**
 * "Explore with demo data" (D-11): replaces local data with a Day-9 program that has real history. Bound only in
 * debug builds; release builds have no implementation, so the affordance cannot appear there.
 */
interface DemoDataLoader {
    suspend fun load()
}
