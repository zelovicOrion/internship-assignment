-- Index for Background Scheduler query
CREATE INDEX idx_timer_status ON timer(status);

-- Index for Status Query
CREATE INDEX idx_timer_timer_id ON timer(timer_id);