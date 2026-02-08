package com.example.stocksync.scheduler;

import com.example.stocksync.service.StockSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockSyncSchedulerTest {

	@Mock
	private StockSyncService stockSyncService;

	@InjectMocks
	private StockSyncScheduler stockSyncScheduler;

	@Test
	void runSync_callsStockSyncService() {
		stockSyncScheduler.runSync();

		verify(stockSyncService).sync();
	}

	@Test
	void runSync_doesNotThrowWhenSyncFails() {
		org.mockito.Mockito.doThrow(new RuntimeException("Sync error")).when(stockSyncService).sync();

		stockSyncScheduler.runSync();

		verify(stockSyncService).sync();
	}
}
