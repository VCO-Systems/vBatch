package model;

import java.io.Serializable;

import javax.persistence.*;

import java.sql.Timestamp;
import java.util.Date;


/**
 * The persistent class for the BATCH_LOG_OK_DTL database table.
 * 
 */
@Entity
@Table(name="BATCH_LOG_OK_DTL")

@NamedQueries({
	@NamedQuery(name="BatchLogOkDtl.findAll", query="SELECT b FROM BatchLogOkDtl b"),
	@NamedQuery(name="BatchLogOkDtl.findByBatchLogId", query="SELECT b FROM BatchLogOkDtl b where b.batchLog = :batchLogId")
})

public class BatchLogOkDtl implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(generator="BatchLogOkDtlGen")
	@SequenceGenerator(name="BatchLogOkDtlGen", sequenceName="BATCH_LOG_OK_DTL_ID_SEQ", allocationSize=1)
	@Column(unique=true, nullable=false)
	private long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable=false)
	private Date ok1;

	private Long pk1;

	private Long pk2;

	private Long pk3;

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

	public Date getOk1() {
		return this.ok1;
	}

	public void setOk1(Date ok1) {
		this.ok1 = ok1;
	}

	public Long getPk1() {
		return this.pk1;
	}

	public void setPk1(Long pk1) {
		this.pk1 = pk1;
	}

	public Long getPk2() {
		return this.pk2;
	}

	public void setPk2(Long pk2) {
		this.pk2 = pk2;
	}

	public Long getPk3() {
		return this.pk3;
	}

	public void setPk3(Long pk3) {
		this.pk3 = pk3;
	}

	public BatchLog getBatchLog() {
		return this.batchLog;
	}

	public void setBatchLog(BatchLog batchLog) {
		this.batchLog = batchLog;
	}

}