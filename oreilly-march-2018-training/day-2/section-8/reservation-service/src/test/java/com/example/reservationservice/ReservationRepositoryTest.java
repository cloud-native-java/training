package com.example.reservationservice;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class ReservationRepositoryTest {

	@Autowired
	private ReservationRepository repo;

	@Autowired
	private TestEntityManager testEntityManager;

	@Test
	public void finder() throws Exception {

		Reservation r = this.testEntityManager.persistFlushFind(new Reservation(null, "Josh"));
		Collection<Reservation> rs = this.repo.findByReservationName("Josh");
		Assertions.assertThat(rs.size()).isEqualTo(1);
		Assertions.assertThat(rs.iterator().next().getId()).isNotNull();
		Assertions.assertThat(rs.iterator().next().getReservationName()).isEqualTo("Josh");
	}
}

