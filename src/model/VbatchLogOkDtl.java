package model;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;


/**
 * The persistent class for the VBATCH_LOG_OK_DTL database table.
 * 
 */
@Entity
@Table(name="VBATCH_LOG_OK_DTL")
@NamedQuery(name="VbatchLogOkDtl.findAll", query="SELECT v FROM VbatchLogOkDtl v")
public class VbatchLogOkDtl implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(unique=true, nullable=false)
	private long id;

	@Column(nullable=false, length=150)
	private String ok;

	@Column(nullable=false)
	private BigDecimal pk1;

	@Column(nullable=false)
	private BigDecimal pk2;

	@Column(nullable=false)
	private BigDecimal pk3;

	//bi-directional many-to-one association to VbatchLog
	@ManyToOne
	@JoinColumn(name="VBATCH_LOG_ID", nullable=false)
	private VbatchLog vbatchLog;

	public VbatchLogOkDtl() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getOk() {
		return this.ok;
	}

	public void setOk(String ok) {
		this.ok = ok;
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

	public VbatchLog getVbatchLog() {
		return this.vbatchLog;
	}

	public void setVbatchLog(VbatchLog vbatchLog) {
		this.vbatchLog = vbatchLog;
	}

}