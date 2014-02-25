package model;

import java.io.Serializable;
import javax.persistence.*;
import java.sql.Timestamp;
import java.math.BigDecimal;


/**
 * The persistent class for the BATCH_LOG_OK_DTL database table.
 * 
 */
@Entity
@Table(name="BATCH_LOG_OK_DTL")
@NamedQuery(name="BatchLogOkDtl.findAll", query="SELECT b FROM BatchLogOkDtl b")
public class BatchLogOkDtl implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(unique=true, nullable=false)
	private long id;

	@Column(nullable=false)
	private Timestamp ok1;

	private BigDecimal pk1;

	private BigDecimal pk2;

	private BigDecimal pk3;

	//bi-directional many-to-one association to BatchLog
	@ManyToOne
	@JoinColumn(name="BATCH_LOG_ID", nullable=false)
	private BatchLog batchLog;

	public BatchLogOkDtl() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Timestamp getOk1() {
		return this.ok1;
	}

	public void setOk1(Timestamp ok1) {
		this.ok1 = ok1;
	}

	public BigDecimal getPk1() {
		return this.pk1;
	}

	public void setPk1(BigDecimal pk1) {
		this.pk1 = pk1;
	}

	public BigDecimal getPk2() {
		return this.pk2;
	}

	public void setPk2(BigDecimal pk2) {
		this.pk2 = pk2;
	}

	public BigDecimal getPk3() {
		return this.pk3;
	}

	public void setPk3(BigDecimal pk3) {
		this.pk3 = pk3;
	}

	public BatchLog getBatchLog() {
		return this.batchLog;
	}

	public void setBatchLog(BatchLog batchLog) {
		this.batchLog = batchLog;
	}

}