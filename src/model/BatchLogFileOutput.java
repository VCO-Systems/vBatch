package model;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;


/**
 * The persistent class for the BATCH_LOG_FILE_OUTPUT database table.
 * 
 */
@Entity
@Table(name="BATCH_LOG_FILE_OUTPUT")
@NamedQuery(name="BatchLogFileOutput.findAll", query="SELECT b FROM BatchLogFileOutput b")
public class BatchLogFileOutput implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(unique=true, nullable=false)
	private long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="CREATE_DT", nullable=false)
	private Date createDt;

	@Column(length=150)
	private String filename;

	@Column(name="NUM_RECORDS")
	private BigDecimal numRecords;

	//bi-directional many-to-one association to BatchLog
	@ManyToOne
	@JoinColumn(name="BATCH_LOG_ID", nullable=false)
	private BatchLog batchLog;

	public BatchLogFileOutput() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Date getCreateDt() {
		return this.createDt;
	}

	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	public String getFilename() {
		return this.filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public BigDecimal getNumRecords() {
		return this.numRecords;
	}

	public void setNumRecords(BigDecimal numRecords) {
		this.numRecords = numRecords;
	}

	public BatchLog getBatchLog() {
		return this.batchLog;
	}

	public void setBatchLog(BatchLog batchLog) {
		this.batchLog = batchLog;
	}

}